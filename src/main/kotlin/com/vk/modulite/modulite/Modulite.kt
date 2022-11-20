package com.vk.modulite.modulite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.ModuliteRestrictionChecker.places
import com.vk.modulite.psi.extensions.files.containingComposerPackage
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.psi.extensions.yaml.addDependencies
import com.vk.modulite.psi.extensions.yaml.moduliteNamePsi
import com.vk.modulite.psi.extensions.yaml.replaceDependencies
import com.vk.modulite.services.*
import com.vk.modulite.utils.normalizedFqn
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence
import java.io.File

/**
 * Class representing any modulite in the code.
 */
data class Modulite(
    override var name: String,
    override val description: String,
    override val path: String,
    override val namespace: Namespace,
    val requires: ModuliteRequires,
    override val exportList: List<SymbolName>,
    override val forceInternalList: List<SymbolName>,
    val allowedInternalAccess: Map<SymbolName, List<SymbolName>>,
) : ModuliteBase() {

    val containInPackage by lazy {
        containingPackage != null
    }

    val containingPackage by lazy {
        configFile()?.containingComposerPackage(project)
    }

    /**
     * @return folded name like @.../name if parent is not null
     */
    fun foldedName() = if (parent() != null)
        "@…/" + name.split('/').last()
    else
        name

    /**
     * @return the parent module, or null if the module has no parent.
     */
    fun parent(): Modulite? {
        val parts = name.split("/")
        if (parts.size < 2) return null
        val parentName = parts.dropLast(1).joinToString("/")
        return ModuliteIndex.getInstance(project).getModulite(parentName)
    }

    /**
     * @return the actual file tree parent module, or null if the module has no parent.
     */
    fun actualParent(): Modulite? {
        val dir = directory()?.parent
        return dir?.containingModulite(project)
    }

    /**
     * @return child modules.
     */
    fun children(): List<Modulite> {
        val modulites = ModuliteIndex.getInstance(project).getModulites()
        val depth = depth()
        return modulites.filter {
            it.depth() == depth + 1 && it.name.startsWith("$name/")
        }
    }

    /**
     * Checks if the current module is some child module.
     */
    fun isNotRoot(): Boolean {
        return name.contains("/")
    }

    /**
     * Checks if the current module is exported from its parent.
     *
     * If the module has no parent, returns false.
     */
    fun isExportedFromParent(): Boolean {
        val parent = parent() ?: return false
        return parent.isExportChild(this)
    }

    /**
     * Проверяет что модуль может быть использован из переданного глобального контекста.
     *
     * Модуль можно использовать, если он экспортирован из своего родителя, который
     * в свою очередь экспортирован из своего.
     *
     * Если некий модуль А не экспортирован из родителя Б, но родитель Б сам экспортирован
     * и видим из переданного контекста, то чтобы модуль А стал видим из контекста, нужно
     * разрешить использование А из переданного контекста посредством allow-internal-access.
     */
    fun canUseGlobally(context: ModuliteRestrictionChecker.Context): Boolean {
        if (isRoot()) {
            // Глобальные модули могут использовать все.
            return true
        }

        val parent = parent() ?: return true
        return parent.isExportModulite(this, context)
    }

    // Проверяет что можно использовать переданный модуль
    // из текущего.
    // Например:
    //   Текущий модуль:    @foo
    //   Переданный модуль: @foo/bar/boo
    //
    // Любой модуль может использовать всех своих прямых потомков
    // и все экспортированные подмодули этого потомка.
    // А также все прямые потомки своих родителей и экспортированные
    // подмодули этих потомков.
    fun canUse(other: Modulite): Boolean {
        // Если переданный модуль является родителем текущего
        // то переданный модуль может быть использован всегда.
        if (other.contains(this)) {
            return true
        }

        // Если текущий модуль лежит в каком-то модуле
        // и переданный модуль лежит в этом модуле,
        // то переданный модуль всегда можно использовать.
        if (haveCommonParent(other)) {
            return true
        }

        // TODO:
//        val directChildOfParent = parent()?.isDirectChild(other) == true
//        if (directChildOfParent) {
//            return true
//        }

        if (this == other) {
            // Модуль может использовать сам себя.
            return true
        }
        if (other.isRoot()) {
            // Глобальные модули могут использовать все.
            return true
        }
        if (isDirectChild(other)) {
            // Модуль может использовать всех своих прямых потомков.
            return true
        }

        val parent = other.parent() ?: return true

        // Проверяем что родительский модуль экспортирует проверяемый модуль.
        // Или он разрешен для текущего модуля в allow-internal-access.
        val exportedOrAllowed = parent.isExportChild(other) || parent.isAllow(other, this)

        // Проверяем можем ли мы использовать родительский модуль.
        val canUseParent = canUse(parent)

        return exportedOrAllowed && canUseParent
    }

    private fun haveCommonParent(other: Modulite): Boolean {
        val parts = name.split("/")
        val otherParts = other.name.split("/")

        if (parts.size < 2 || otherParts.size < 2) {
            return false
        }

        return parts[0] == otherParts[0]
    }

    private fun isAllow(child: Modulite, forModulite: Modulite): Boolean {
        val symbols = allowedInternalAccess.getOrDefault(forModulite.symbolName(), emptyList())
        return symbols.any { it.name == child.name }
    }

    private fun isAllow(child: Modulite, context: ModuliteRestrictionChecker.Context): Boolean {
        val placesToCheck = context.places()

        // Если хотя бы для одного места разрешено, то можно использовать.
        return placesToCheck.any { place ->
            val symbols = allowedInternalAccess.getOrDefault(place, emptyList())
            symbols.any { it.name == child.name }
        }
    }

    /**
     * Checks if the current module and the [other] module have a common parent.
     */
    fun hasCommonParent(other: Modulite): Boolean {
        val parts = name.split("/")
        val otherParts = other.name.split("/")
        if (parts.size < 2 || otherParts.size < 2) return false
        return parts[0] == otherParts[0]
    }

    /**
     * Checks if the [other] module is a direct child of the current module.
     */
    fun isDirectChild(other: Modulite): Boolean {
        return this == other.parent()
    }

    /**
     * @return all symbols that are defined in the module.
     */
    fun symbols(): List<SymbolName> {
        val dir = directory() ?: return emptyList()
        return ModuliteSymbolsCollector.getInstance(project).collect(dir)
    }

    /**
     * @return all symbols on which the current module depends.
     *
     * @param collapseModuleSymbols if true, then all symbols from other modules
     *                              will be collapsed into the module name.
     */
    fun dependencies(collapseModuleSymbols: Boolean = true): ModuliteDeps {
        val dir = directory() ?: return ModuliteDeps(project, this)
        return ModuliteDependenciesCollector.getInstance(project)
            .collect(dir, collapseModuleSymbols = collapseModuleSymbols)
    }

    fun addDependencies(deps: ModuliteDeps) = configPsiFile()?.addDependencies(deps)

    fun addDependencies(symbols: List<SymbolName>) = addDependencies(ModuliteDeps(project, this, symbols))

    fun addDependencies(symbol: SymbolName) = addDependencies(listOf(symbol))

    fun replaceDependencies(deps: ModuliteDeps, onReady: (ModuliteDepsDiff) -> Unit) =
        configPsiFile()?.replaceDependencies(deps, onReady)

    /**
     * Checks if the [namespace] is valid for the module.
     */
    fun isValidNamespace(other: Namespace): Boolean {
        return namespace.isGlobal() || namespace == other || other.subOf(namespace)
    }

    fun containsInRequires(name: SymbolName): Boolean {
        // Если используется поле или константа класса, то достаточно
        // проверить что в зависимостях есть их имя или их класс.
        if (name.kind == SymbolName.Kind.Field || name.kind == SymbolName.Kind.ClassConstant) {
            val className = name.className()
            return requires.symbols.contains(name) || requires.symbols.contains(className)
        }

        // Сначала ищем ключ по абсолютному пути, если не найдено, то продуем искать по относительному пути
        return requires.symbols.contains(name) || requires.symbols.contains(name.relative(this))
    }

    fun contains(file: VirtualFile): Boolean {
        val moduleFolder = File(path).parent

        // TODO: fix this somehow
        if (ApplicationManager.getApplication().isUnitTestMode) {
            val path = file.path
            val folder = moduleFolder.removeSuffix("/hints/Module")
            return path == folder || path.startsWith("$folder${File.separator}")
        }

        return file.path == moduleFolder || file.path.startsWith("$moduleFolder${File.separator}")
    }

    fun contains(file: PsiFile) = contains(file.virtualFile)

    fun contains(element: PsiElement): Boolean {
        if (element is Variable) {
            // Модули не могут содержать глобальные переменные.
            // Такие переменные всегда лежат в глобальном скоупе.
            return false
        }

        return contains(element.containingFile)
    }

    fun contains(other: Modulite) = other.name.startsWith("$name/")

    fun namePsi() = configPsiFile()?.moduliteNamePsi()

    fun directory() = configFile()?.parent

    fun configPsiFile(): YAMLFile? = super.configPsiFileImpl()

    fun symbolInAllowedInternalAccess(
        element: PhpNamedElement,
        context: ModuliteRestrictionChecker.Context,
        reference: PhpReference,
    ): Boolean {
        val name = element.symbolName(reference)

        if (context.modulite != null) {
            if (containsInAllowedInternalAccessKey(context.modulite.symbolName(), name)) {
                return true
            }
            if (containsInAllowedInternalAccessKey(context.modulite.composerPackageBasedName(), name)) {
                return true
            }
        }

        if (context.function != null) {
            val fqn = context.function.symbolName()
            if (containsInAllowedInternalAccessKey(fqn, name)) {
                return true
            }
        }

        if (context.klass != null) {
            if (containsInAllowedInternalAccessKey(context.klass.symbolName(reference), name)) {
                return true
            }
        }

        return false
    }

    private fun containsInAllowedInternalAccessKey(key: SymbolName, name: SymbolName): Boolean {
        // Сначала ищем ключ по абсолютному пути, если не найдено, то продуем искать по относительному пути
        val symbols = allowedInternalAccess[key] ?: allowedInternalAccess[key.relative(this)] ?: return false

        if (name.kind == SymbolName.Kind.Class) {
            // В случае если у нас уже есть разрешение для какого-то члена класса
            // то автоматически это разрешение добавляется и для самого класса.
            val containsAnyMember = containsInAllowedInternalAccess(symbols) {
                it.isClassMember() && name.equals(it.className(), this)
            }
            if (containsAnyMember) {
                return true
            }
        }

        if (name.isClassMember()) {
            // Для членов класса проверяем, есть ли класс в списке.
            // Если есть, то считаем, что метод разрешен для внешнего доступа
            // в текущем модуле.
            val className = name.className()
            if (containsInAllowedInternalAccess(symbols, className)) {
                return true
            }
        }

        return containsInAllowedInternalAccess(symbols, name)
    }

    private fun containsInAllowedInternalAccess(symbols: List<SymbolName>, name: SymbolName) =
        containsInAllowedInternalAccess(symbols) { symbol ->
            name.equals(symbol, this)
        }

    private fun containsInAllowedInternalAccess(symbols: List<SymbolName>, cb: (SymbolName) -> Boolean) =
        symbols.any { symbol ->
            cb(symbol)
        }

    private fun isExportModulite(child: Modulite, context: ModuliteRestrictionChecker.Context): Boolean {
        if (isRoot()) {
            return isExportChild(child) || isAllow(child, context)
        }

        // Проверяем что родительский модуль экспортирует модуль.
        // Или он разрешен для текущего модуля в allow-internal-access.
        val exportedOrAllowed = isExportChild(child) || isAllow(child, context)
        if (!exportedOrAllowed) {
            return false
        }

        val parent = parent() ?: return false

        // Проверяем что родитель экспортирует текущий модуль.
        return parent.isExportModulite(this, context)
    }

    private fun isExportChild(child: Modulite): Boolean {
        return exportList.any { it.name == child.name }
    }

    private fun depth() = name.count { it == '/' }

    private fun isRoot() = !name.contains('/')

    fun composerPackageBasedName(): SymbolName {
        if (containingPackage == null) {
            return symbolName()
        }

        return SymbolName(containingPackage!!.name + "/" + name, SymbolName.Kind.Modulite)
    }

    override fun symbolName() = SymbolName(name, SymbolName.Kind.Modulite)

    override fun toString() = name

    companion object {
        fun fromYamlPsi(file: YAMLFile): Modulite {
            var name = ""
            var description = ""
            var namespace = Namespace()

            var exportList = emptyList<SymbolName>()
            var forceInternalList = emptyList<SymbolName>()
            var requiresList = emptyList<SymbolName>()
            val allowedInternalAccess = mutableMapOf<SymbolName, List<SymbolName>>()

            val topLevelsKey = YAMLUtil.getTopLevelKeys(file)
            topLevelsKey.forEach { keyValue ->
                when (keyValue.keyText) {
                    "name"                  -> {
                        name = keyValue.valueText.unquote()
                    }

                    "description"           -> {
                        description = keyValue.valueText.unquote()
                    }

                    "namespace"             -> {
                        namespace = Namespace(keyValue.valueText.unquote())
                    }

                    "export"                -> {
                        exportList = yamlArrayToList(keyValue)
                    }

                    "force-internal"        -> {
                        forceInternalList = yamlArrayToList(keyValue)
                    }

                    "require"               -> {
                        requiresList = yamlArrayToList(keyValue)
                    }

                    "allow-internal-access" -> {
                        if (keyValue.value == null) return@forEach
                        if (keyValue.value !is YAMLMapping) return@forEach

                        val mapping = keyValue.value as YAMLMapping
                        mapping.keyValues.forEach forEachKeys@{
                            val symbolsList = yamlArrayToList(it)
                            val key = it.keyText.normalizedFqn()
                            val symbolName = SymbolName(key)

                            allowedInternalAccess[symbolName] = symbolsList
                        }
                    }
                }
            }

            return Modulite(
                name, description,
                file.virtualFile.path,
                namespace,
                ModuliteRequires(requiresList),
                exportList,
                forceInternalList,
                allowedInternalAccess
            ).also { it.project = file.project }
        }

        private fun yamlArrayToList(keyValue: YAMLKeyValue): List<SymbolName> {
            val list = mutableListOf<SymbolName>()

            if (keyValue.value == null) return emptyList()
            if (keyValue.value !is YAMLSequence) return emptyList()

            val seq = keyValue.value as YAMLSequence

            seq.items.forEach {
                val text = it.value?.text ?: return@forEach

                val symbolName = SymbolName(text.unquote(), fromYaml = true)
                list.add(symbolName)
            }

            return list
        }
    }
}
