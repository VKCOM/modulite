package com.vk.modulite.psi.extensions.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.php.lang.psi.elements.*
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteRestrictionChecker
import com.vk.modulite.modulite.ModuliteRestrictionChecker.kind
import com.vk.modulite.modulite.ModuliteRestrictionChecker.name
import com.vk.modulite.notifications.ModuliteNotification
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.allowInternalAccessPsi
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.createAllowInternalAccess
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.createExport
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.createRequires
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.exportPsi
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.forceInternalPsi
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.namePsi
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.replaceAllowInternalAccess
import com.vk.modulite.psi.extensions.yaml.ConfigBuilder.requirePsi
import com.vk.modulite.services.FindUsagesService
import com.vk.modulite.services.ModuliteDeps
import com.vk.modulite.services.ModuliteDepsDiff
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.*
import com.vk.modulite.utils.Utils.runModal
import com.vk.modulite.utils.Utils.runTransparent
import com.vk.modulite.utils.Utils.writeCommand
import com.vk.modulite.utils.YamlUtils.addElement
import com.vk.modulite.utils.YamlUtils.addTopLevelKey
import com.vk.modulite.utils.YamlUtils.createSequence
import com.vk.modulite.utils.YamlUtils.removeElement
import com.vk.modulite.utils.YamlUtils.removeElements
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence

annotation class OnlyInsideModification

private fun YAMLFile.mod(project: Project, name: String, modification: () -> Unit) = writeCommand(project) {
    CommandProcessor.getInstance().executeCommand(
        project,
        {
            CommandProcessor.getInstance().markCurrentCommandAsGlobal(project)
            runTransparent {
                modification()
                commitChanges()
            }
        },
        name,
        "modulite.yaml.modification.${name.replace(" ", ".")}",
        UndoConfirmationPolicy.REQUEST_CONFIRMATION
    )
}

private fun YAMLFile.commitChanges() = writeCommand(project) {
    val document = PsiDocumentManager.getInstance(project).getDocument(this) ?: return@writeCommand
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
}

fun YAMLFile.moduliteName(): String? {
    val name = namePsi ?: return null
    return name.value?.text?.unquote()
}

fun YAMLFile.moduliteNamePsi(): ModuliteNamePsi? {
    val name = namePsi ?: return null
    return name.value as? ModuliteNamePsi
}

/**
 * @return модуль представляемый файлом.
 */
fun YAMLFile.relatedModulite(): Modulite? {
    val name = moduliteName() ?: return null
    return ModuliteIndex.getInstance(project).getModulite(name)
}

/**
 * Разрешает использовать внутренний [modulite] из [place].
 */
fun YAMLFile.allowModulite(place: String, modulite: ModuliteNamePsi) = mod(project, "allow modulite for $place") {
    val name = modulite.symbolName().relative(relatedModulite())
    allowElementName(place, name.toYaml())
}

/**
 * Разрешает использовать внутренний [element] из [place].
 */
fun YAMLFile.allowElement(place: String, element: PhpNamedElement) {
    allowElements(listOf(place), element)
}

/**
 * Разрешает использовать внутренний [element] из [places].
 */
fun YAMLFile.allowElements(places: List<String>, element: PhpNamedElement) = mod(project, "allow elements for external places") {
    places.forEach { place ->
        val name = element.symbolName().relative(relatedModulite()).toYaml()
        allowElementName(place, name)
    }
}

@OnlyInsideModification
private fun YAMLFile.allowElementName(place: String, name: String) {
    val allowKeyValue = allowInternalAccessPsi
    if (allowKeyValue == null) {
        createAllowInternalAccess(place, createSequence(project, name))
        return
    }

    val allowMapping = allowKeyValue.value as? YAMLMapping
    if (allowMapping == null) {
        replaceAllowInternalAccess(place, createSequence(project, name))
        return
    }

    val placeKeyValue = allowMapping.getKeyValueByKey(place)
    if (placeKeyValue == null) {
        allowMapping.putKeyValue(
            YamlUtils.createKeyValue(
                place.quote(),
                createSequence(project, name)
            )
        )
        return
    }

    val allowedList = placeKeyValue.value as YAMLSequence?
    if (allowedList == null) {
        val node = YamlUtils.createKeyValue(place.quote(), createSequence(project, name))
        placeKeyValue.replace(node)
        return
    }

    allowedList.addElement(project, name)
}

/**
 * Разрешает использовать внутренний [element] из [modulite].
 */
fun YAMLFile.allowElementForModulite(element: PhpNamedElement, modulite: Modulite) {
    allowElement(modulite.name, element)
}

/**
 * Разрешает использовать внутренний [element] из [context].
 */
fun YAMLFile.allowElementForContexts(element: PhpNamedElement, contexts: List<ModuliteRestrictionChecker.Context>) {
    val places = contexts.map { context ->
        when {
            context.composerPackage != null -> context.composerPackage.name
            context.modulite != null        -> context.modulite.name
            context.function != null        -> context.function.symbolName().toYaml()
            context.klass != null           -> context.klass.symbolName().toYaml()
            else                            -> ""
        }
    }

    allowElements(places, element)
}

fun YAMLFile.requestAllowExternalUsages(element: PhpNamedElement) = invokeLater {
    if (ApplicationManager.getApplication().isUnitTestMode) {
        return@invokeLater
    }

    val project = element.project
    val result = Messages.showOkCancelDialog(
        project,
        """
            Probably, there are some usages of this element in the existing code. 
            Without adding them to exceptions, that usages will be treated as errors.
            
            Plugin can search for usages and add them to exceptions. Process?
        """.trimIndent(),
        "Making an Element Internal",
        "Yes",
        "No",
        null
    )


    if (result == Messages.CANCEL) {
        return@invokeLater
    }

    runModal(project, "Find usages of ${element.name} to allow external access") {
        val countExternalUsages = runReadAction { allowExternalUsages(element) }

        invokeLater il@{
            if (countExternalUsages == 0) {
                ModuliteNotification("No external usages found, nothing to allow")
                    .withTitle("Element successfully made internal")
                    .show()
                return@il
            }

            ModuliteNotification("Allowed $countExternalUsages usages")
                .withTitle("Element successfully allowed for external access")
                .show()
        }
    }
}

fun YAMLFile.allowExternalUsages(element: PhpNamedElement): Int {
    val currentModulite = containingModulite()
    val usages = FindUsagesService.getInstance(element.project).findUsages(element)
    val contexts = usages
        .filter { it.parent !is PhpUse }
        .mapNotNull {
            val ctx = ModuliteRestrictionChecker.createContext(it)
            // Те места использования внутри текущего модуля не нужно добавлять в исключения
            if (ctx.modulite == currentModulite) {
                return@mapNotNull null
            }

            ctx
        }
        .sortedBy { it.name() }
        .sortedBy { it.kind() }

    invokeLater {
        allowElementForContexts(element, contexts)
    }

    return contexts.size
}

/**
 * Делает внутренний [element] недоступным из других мест.
 *
 * Можно вызывать только внутри некоторой модификации конфига.
 *
 * @see YAMLFile.mod
 */
@OnlyInsideModification
private fun YAMLFile.disallowElement(element: PhpNamedElement) {
    val allowKeyValue = allowInternalAccessPsi ?: return
    val allowMapping = allowKeyValue.value as YAMLMapping? ?: return

    allowMapping.keyValues.forEach { keyValue ->
        if (keyValue.value == null) return@forEach
        if (keyValue.value !is YAMLSequence) return@forEach

        val seq = keyValue.value as YAMLSequence
        val elementFqn = element.symbolName()

        seq.removeElement(elementFqn.toYaml())
        seq.removeElement(elementFqn.relative(relatedModulite()).toYaml())

        if (seq.items.isEmpty()) {
            keyValue.delete()
        }
    }
}

/**
 * Добавляет переданные [symbols] в список экспортированных элементов.
 */
fun YAMLFile.addExports(symbols: List<SymbolName>) = mod(project, "add exports") {
    exportNames(symbols)
}

/**
 * Делает переданные [symbols] внутренним.
 */
fun YAMLFile.removeExports(symbols: List<SymbolName>) = mod(project, "remove exports") {
    makeNamesInternal(symbols)
}

/**
 * Делает [modulite] экспортируемым из родителя.
 */
fun YAMLFile.makeModuliteExport(modulite: ModuliteNamePsi) = mod(project, "make modulite exported") {
    val name = modulite.symbolName().relative(relatedModulite())
    exportName(name)
}

/**
 * Делает [element] экспортируемым из своего модуля.
 */
fun YAMLFile.makeElementExport(element: PhpNamedElement) = mod(project, "make element exported") {
    if (element is Variable) {
        // Переменные не могут быть экспортированы из модуля,
        // так как они ему не принадлежат.
        return@mod
    }

    // Когда элемент становится публичным, то все явные разрешения
    // на его использования их других мест должны быть удалены.
    disallowElement(element)

    if (element is Method || element is Field) {
        val internalKeyValue = forceInternalPsi ?: return@mod
        val internalList = internalKeyValue.value as YAMLSequence? ?: return@mod

        val name = element.symbolName()
        internalList.removeElement(name.toYaml())
        internalList.removeElement(name.relative(relatedModulite()).toYaml())

        return@mod
    }

    val name = element.symbolName().relative(relatedModulite())
    exportName(name)
}

@OnlyInsideModification
private fun YAMLFile.exportName(name: SymbolName) {
    exportNames(listOf(name))
}

@OnlyInsideModification
private fun YAMLFile.exportNames(names: List<SymbolName>) {
    val exportKeyValue = exportPsi
    if (exportKeyValue == null) {
        createExport(names)
        return
    }

    val exportList = exportKeyValue.value as YAMLSequence?
    if (exportList == null) {
        createExport(names)
        return
    }

    val exports = exportList().toMutableList()
    exports.addAll(names)

    createExport(exports)
}

fun YAMLFile.makeModuliteInternal(element: ModuliteNamePsi) = mod(project, "make modulite internal") {
    val exportKeyValue = exportPsi ?: return@mod
    val exportList = exportKeyValue.value as YAMLSequence? ?: return@mod

    val name = element.symbolName()
    makeNameInternal(name, exportList)
}

fun YAMLFile.makeElementInternal(element: PhpNamedElement) = mod(project, "make element internal") {
    val exportKeyValue = exportPsi ?: return@mod
    val exportList = exportKeyValue.value as YAMLSequence? ?: return@mod

    if (element is Method || element is Field) {
        val name = element.symbolName().relative(relatedModulite()).toYaml()

        val internalKeyValue = forceInternalPsi
        if (internalKeyValue == null) {
            addTopLevelKey("force-internal", createSequence(project, name))
            requestAllowExternalUsages(element)
            return@mod
        }

        val internalList = internalKeyValue.value as YAMLSequence?
        if (internalList == null) {
            val node = YamlUtils.createKeyValue("force-internal", createSequence(project, name))
            internalKeyValue.replace(node)
            requestAllowExternalUsages(element)
            return@mod
        }

        internalList.addElement(project, name)

        requestAllowExternalUsages(element)
        return@mod
    }

    val name = element.symbolName()
    removeExplicitlyMemberInternal(element)
    requestAllowExternalUsages(element)
    makeNameInternal(name, exportList)
}

/**
 * When a class becomes internal, all explicit hiding of its members can be removed.
 */
@OnlyInsideModification
private fun YAMLFile.removeExplicitlyMemberInternal(element: PhpNamedElement) {
    if (element !is PhpClass) {
        return
    }

    val internalSeq = forceInternalPsi?.value as YAMLSequence? ?: return
    val symbols = internalSeq.extractSymbolsList()
    val className = element.symbolName()
    val modulite = containingModulite() ?: return

    val classMembers = symbols
        .filter { it.isClassMember() && it.className().absolutize(modulite) == className }
        .map { it.toYaml() }

    internalSeq.removeElements(classMembers)
}

@OnlyInsideModification
private fun YAMLFile.makeNameInternal(name: SymbolName, exportList: YAMLSequence) {
    exportList.removeElement(name.toYaml())
    exportList.removeElement(name.relative(relatedModulite()).toYaml())
}

@OnlyInsideModification
private fun YAMLFile.makeNamesInternal(names: List<SymbolName>) {
    val exportList = exportPsi?.value as YAMLSequence? ?: return

    val modulite = relatedModulite()
    val normalized = names.map { it.toYaml() } + names.map { it.relative(modulite).toYaml() }
    exportList.removeElements(normalized)
}

fun YAMLFile.replaceDependencies(deps: ModuliteDeps, cb: (ModuliteDepsDiff) -> Unit) = mod(project, "replace dependencies") {
    val currentRequires = dependencies()
    val diff = deps.diff(currentRequires)

    if (diff.isEmpty()) {
        // Нет смысла заменять зависимости
        // если они идентичны существующим.
        cb(diff)
        return@mod
    }

    val requiresKeyValue = requirePsi
    if (requiresKeyValue != null) {
        requiresKeyValue.value?.delete()
    }

    addDependencies(deps)

    cb(diff)
}

/**
 * Добавляет новые зависимости в модуль.
 */
fun YAMLFile.addDependencies(deps: ModuliteDeps) = mod(project, "add dependencies") {
    if (deps.isEmpty()) {
        return@mod
    }

    if (requirePsi == null) {
        createRequires(deps.symbols)
        return@mod
    }

    val unionDeps = dependencies().union(deps)

    val moduliteName = moduliteName()
    val symbols = unionDeps.symbols
        .filter { it.name != moduliteName }

    createRequires(symbols)
}

/**
 * Удаляет зависимости от [symbols] из модуля.
 */
fun YAMLFile.removeDependencies(symbols: List<SymbolName>) = mod(project, "remove dependencies") {
    val requiresSeq = requirePsi?.value as? YAMLSequence ?: return@mod

    val normalized = symbols.map { it.toYaml() }
    requiresSeq.removeElements(normalized)
}

fun YAMLFile.diffDependencies(deps: ModuliteDeps): ModuliteDepsDiff {
    val currentRequires = dependencies()
    return deps.diff(currentRequires)
}

/**
 * @return список элементов от которых зависит модуль.
 */
fun YAMLFile.dependencies(): ModuliteDeps {
    val requiresSeq = requirePsi?.value as? YAMLSequence ?: return ModuliteDeps(project, relatedModulite())

    val symbols = requiresSeq.extractSymbolsList()
    return ModuliteDeps(project, relatedModulite(), symbols)
}

/**
 * @return список символов которые экспортированы из модуля.
 */
fun YAMLFile.exportList(): List<SymbolName> {
    val exportSeq = exportPsi?.value as? YAMLSequence ?: return emptyList()

    return exportSeq.extractSymbolsList()
}

fun YAMLSequence.extractSymbolsList(): List<SymbolName> {
    return items.mapNotNull {
        val value = it.value ?: return@mapNotNull null
        SymbolName(value.text.unquote(), fromYaml = true)
    }
}
