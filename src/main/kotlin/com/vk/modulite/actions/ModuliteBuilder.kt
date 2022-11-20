package com.vk.modulite.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.rename.RenameProcessor
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.actions.dialogs.ModuliteBuilderData
import com.vk.modulite.actions.dialogs.ModuliteWizardDialog
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteDependenciesManager
import com.vk.modulite.notifications.ModuliteNotification
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.*
import com.vk.modulite.services.ModuliteDeps
import com.vk.modulite.services.ModuliteDependenciesCollector
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.services.ModuliteSymbolsCollector
import com.vk.modulite.utils.Utils.runBackground
import com.vk.modulite.utils.gotItNotification
import com.vk.modulite.utils.toKebabCase
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ModuliteBuilder(private val project: Project) {
    companion object {
        private val LOG = logger<ModuliteBuilder>()
    }

    fun startBuild(folder: VirtualFile, fromSource: Boolean) {
        val parentModulite = folder.containingModulite(project)

        if (fromSource) {
            createModuliteFromSource(folder, parentModulite) { data: ModuliteData ->
                val dialog = ModuliteWizardDialog(
                    project, parentModulite, folder, data.generatedName,
                    data.symbols, data.namespace, fromSource = true,
                )
                if (!dialog.showAndGet()) {
                    return@createModuliteFromSource null
                }

                dialog.data()
            }
            return
        }

        createRawModulite(folder) {
            val dialog = ModuliteWizardDialog(project, parentModulite, folder, fromSource = false)
            if (!dialog.showAndGet()) {
                return@createRawModulite null
            }

            dialog.data()
        }
    }

    fun createModuliteFromSource(
        folder: VirtualFile,
        parentModulite: Modulite?,
        dataProvider: (ModuliteData) -> ModuliteBuilderData?,
    ) {
        runWriteAction {
            try {
                val moduliteName = generateModuliteName(folder)

                buildConfig(parentModulite, folder, moduliteName, dataProvider) { exceptModulite ->
                    runBackground(project, "Regenerate other modulites requires") {
                        regenerateOtherModulites(exceptModulite)
                    }
                }
            } catch (e: RuntimeException) {
                LOG.error("Cannot create .modulite.yaml", e)
                ModuliteNotification(e.message ?: "")
                    .withTitle("Cannot create .modulite.yaml")
                    .show()
            }
        }
    }

    fun createRawModulite(
        folder: VirtualFile,
        dataProvider: () -> ModuliteBuilderData?,
    ) {
        val data = dataProvider() ?: return

        initModuliteFolder(folder, data.folder)
        val moduliteFolder = folder.findChild(data.folder) ?: return
        initConfig(data, moduliteFolder)
    }

    /**
     * Creates a new folder in which the module will be located.
     */
    private fun initModuliteFolder(folder: VirtualFile, subFolder: String) {
        runWriteAction {
            folder.createChildDirectory(this, subFolder)
        }
    }

    private fun regenerateOtherModulites(exceptModulite: String) {
        gotItNotification(
            project, "regenerate.other.modulites", "Other modulites dependencies need to be regenerated",
            """
                When creating a new modulite, it may happen that specific symbols from this 
                modulite occur in 'require' of other modulites. Once a new modulite is created, 
                those modulites must require the new modulite, not its symbols. 
                <br>
                <br>
                Therefore, after creating a new modulite, the plugin will process 
                all the modulites in the background and show summary.
            """.trimIndent()
        )

        val changedModulites = Collections.synchronizedList(mutableListOf<Modulite>())

        val otherModulites = runReadAction {
            ModuliteIndex.getInstance(project).getModulites().filter { it.name != exceptModulite }
        }

        val atomic = AtomicInteger(otherModulites.size)

        otherModulites.forEach { modulite ->
            ModuliteDependenciesManager.actualRequiresDiff(project, modulite) { diff ->
                if (diff == null) {
                    atomic.addAndGet(-1)
                    return@actualRequiresDiff
                }
                // Если в диффе добавился модуль который мы только что создали, это означает
                // что в этом модуле есть изменения и необходимо удалить все отдельные символы
                // из только что созданного модуля и добавить только что созданный модуль в зависимости.
                val hasChanges = diff.symbols.any {
                    it.key.kind == SymbolName.Kind.Modulite && it.key.name == exceptModulite && it.value == 1
                }

                if (!hasChanges) {
                    atomic.addAndGet(-1)
                    return@actualRequiresDiff
                }

                val symbolsToDelete = diff.symbols.filter { (symbol, status) ->
                    if (status != -1) return@filter false
                    val elements = symbol.resolve(project)

                    elements.any { it.containingModulite()?.name == exceptModulite }
                }.keys.toList()

                if (symbolsToDelete.isEmpty()) {
                    atomic.addAndGet(-1)
                    return@actualRequiresDiff
                }

                val configFile = modulite.configPsiFile()

                invokeLater {
                    // Добавляем зависимость от нового модуля
                    val moduliteName = SymbolName(exceptModulite, SymbolName.Kind.Modulite)
                    configFile?.addDependencies(ModuliteDeps(project, modulite, moduliteName))

                    // Удаляем все одиночные символы нового модуля
                    configFile?.removeDependencies(symbolsToDelete)
                }

                atomic.addAndGet(-1)
                changedModulites.add(modulite)
            }
        }

        while (atomic.get() > 0) {
            Thread.sleep(100)
        }

        if (changedModulites.isEmpty()) {
            ModuliteNotification("New modulite did not affect the rest of the modulites")
                .withTitle("Regenerated requires of modulites")
                .show()
            return
        }

        val moduleNames = changedModulites.joinToString("<br>") { "- " + it.name }
        ModuliteNotification("Individual symbols in requires have been replaced with require new modulite in:<br><br>$moduleNames")
            .withTitle("Regenerated requires of modulites")
            .show()
    }

    data class ModuliteData(
        val deps: ModuliteDeps? = null,
        val symbols: List<SymbolName> = emptyList(),
        val namespace: Namespace = Namespace("\\"),
        var generatedName: String = "",
    )

    private fun collectData(folder: VirtualFile, onReady: (ModuliteData) -> Unit) {
        runBackground(project, "Collect symbols for modulite config") {
            runReadAction {
                val symbols = ModuliteSymbolsCollector.getInstance(project).collect(folder)
                val deps = ModuliteDependenciesCollector.getInstance(project).collect(folder, symbols)
                val namespace = ModuliteDependenciesCollector.getInstance(project).resolveNamespaceInside(folder)

                invokeLater {
                    onReady(ModuliteData(deps, symbols, namespace))
                }
            }
        }
    }

    /**
     * Generates a module name based on the [folder] in which it
     * will be created.
     *
     * If the module is nested, then the generated name will not
     * have an `@` at the beginning.
     *
     * If a module with the generated name already exists, then the
     * number 1 will be added to it to make it unique.
     *
     * Example:
     *
     *    "Messages" -> "messages"
     *    "ServiceManager" -> "service-manager"
     */
    private fun generateModuliteName(folder: VirtualFile): String {
        val containingModulite = folder.containingModulite(project)

        val kebabFilename = folder.name.toKebabCase()
        val initialName = if (containingModulite != null) {
            kebabFilename
        } else {
            "@$kebabFilename"
        }

        val alreadyExist = ModuliteIndex.getInstance(project).getModulite(initialName) != null
        return initialName + if (alreadyExist)
            "1"
        else
            ""
    }

    private fun buildConfig(
        parent: Modulite?,
        folder: VirtualFile,
        generatedName: String,
        dataProvider: (ModuliteData) -> ModuliteBuilderData?,
        onReady: (String) -> Unit,
    ) {
        collectData(folder) { collectedData ->
            val data = dataProvider(collectedData.apply { this.generatedName = generatedName }) ?: return@collectData

            val modulitesRenames = processNestedModulitesRenames(data, parent, collectedData)
            initConfig(data, folder, collectedData.symbols, collectedData.deps, modulitesRenames, onReady)
        }
    }

    private fun processNestedModulitesRenames(
        data: ModuliteBuilderData,
        parent: Modulite?,
        collectedData: ModuliteData,
    ): MutableMap<String, String> {
        val newParentName = data.name
        val oldParentName = parent?.name ?: ""
        val modulites = collectedData.symbols.filter { it.kind == SymbolName.Kind.Modulite }

        val modulitesRenames = mutableMapOf<String, String>()
        modulites.forEach {
            val namePsi = it.resolve(project).firstOrNull() as? ModuliteNamePsi ?: return@forEach
            val oldName = it.name
            val newName = newParentName + "/" + oldName.removePrefix(oldParentName).removePrefix("@").removePrefix("/")

            RenameProcessor(project, namePsi, newName, false, false).run()

            modulitesRenames[oldName] = newName
        }

        return modulitesRenames
    }

    private fun initConfig(
        data: ModuliteBuilderData,
        folder: VirtualFile,
        symbols: List<SymbolName> = emptyList(),
        deps: ModuliteDeps? = null,
        modulitesRenames: Map<String, String> = mapOf(),
        onReady: (String) -> Unit = {},
    ) {
        val namespace = data.namespace
        val exportSymbols = data.selectedSymbols.map {
            if (it.kind == SymbolName.Kind.Modulite) {
                val newName = modulitesRenames.getOrDefault(it.name, it.name)
                if (newName == it.name) {
                    return@map it
                }
                return@map SymbolName(newName, SymbolName.Kind.Modulite)
            }

            it
        }
        val exportSymbolsString = exportSymbols.joinToString("\n") {
            // Escape dollar sign, because it is used as a part of variable name in the templates.
            val fqn = it.relative(namespace).toYaml()
                .replace("$", "\\\\\\$")
            "  - \"$fqn\""
        }

        val template = FileTemplateManager.getInstance(project).getInternalTemplate(".modulite.yaml")
        val fixedTemplate = template.clone()
        val fixedTemplateText = template.text
            .replace("<modulite_name>", data.name)
            .replace("<description>", data.description ?: "")
            .replace("<namespace>", namespace.toYaml())
            .replace("<exported_symbols>", if (exportSymbolsString.isNotEmpty()) exportSymbolsString + "\n" else "")

        fixedTemplate.text = fixedTemplateText

        val directory = PsiManager.getInstance(project).findDirectory(folder) ?: return

        runWriteAction {
            val file = CreateFileFromTemplateAction.createFileFromTemplate(
                ".modulite.yaml",
                fixedTemplate,
                directory,
                null,
                true,
                emptyMap()
            )

            if (file == null) {
                LOG.warn("Can't create file .modulite.yaml")
                return@runWriteAction
            }

            val virtualFile = file.virtualFile ?: return@runWriteAction

            val modulite = virtualFile.containingModulite(project)
            if (deps != null) {
                modulite?.addDependencies(deps)
            }

            if (data.exported) {
                val moduliteNamePsi = modulite?.namePsi()
                if (moduliteNamePsi != null) {
                    data.parent?.configPsiFile()?.makeModuliteExport(moduliteNamePsi)
                }
            }

            runBackground(project, "Updating parent modulite exports") {
                processParentModuliteExports(data.parent, modulite, symbols)

                invokeLater {
                    FileEditorManager.getInstance(file.project).openFile(virtualFile, true)
                }
                onReady(data.name)
            }
        }
    }

    private fun processParentModuliteExports(parent: Modulite?, modulite: Modulite?, symbols: List<SymbolName>) {
        if (parent == null) return
        if (modulite == null) return

        val (exports, file) = runReadAction {
            val file = parent.configPsiFile() ?: return@runReadAction null
            val exports = file.exportList().map {
                it.absolutize(parent)
            }.toSet()

            exports to file
        } ?: return

        val symbolsToDelete = symbols
            .filter { exports.contains(it.absolutize(modulite)) }
            .map { it.relative(parent) }

        if (symbolsToDelete.isEmpty()) return

        invokeLater {
            file.removeExports(symbolsToDelete)
            file.addExports(listOf(modulite.symbolName()))
            ModuliteDependenciesManager.regenerate(project, file.virtualFile)
        }
    }
}
