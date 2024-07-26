package com.vk.modulite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.containers.TreeNodeProcessingResult
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.FieldImpl
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpDefineImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpUseImpl
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.actions.dialogs.DepsRegenerationResultDialog
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.notifications.ModuliteWarningNotification
import com.vk.modulite.psi.PhpRecursiveElementVisitor
import com.vk.modulite.psi.extensions.files.*
import com.vk.modulite.psi.extensions.php.safeFqn
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.utils.fromKphpPolyfills
import com.vk.modulite.utils.fromStubs
import com.vk.modulite.utils.fromTests
import java.util.*


class ModuliteDepsDiff(
    private val project: Project,
    private val firstModulite: Modulite?,
    symbolsMap: Map<SymbolName, Int>,
) {
    val symbols: Map<SymbolName, Int>

    init {
        val moduliteName = firstModulite?.symbolName()
        symbols = symbolsMap.filter { it.key != moduliteName }
    }

    fun added() = symbols.filter { it.value == 1 }.map { it.key }

    fun removed() = symbols.filter { it.value == -1 }.map { it.key }

    fun showDetails() {
        if (firstModulite == null) {
            LOG.warn("Can't show details for dependencies diff without modulite")
            ModuliteWarningNotification("Please try again")
                .withTitle("Can't show details for regenerated dependencies")
                .show()
            return
        }

        DepsRegenerationResultDialog(project, firstModulite, this).showAndGet()
    }

    fun shortInfo(): String {
        if (symbols.isEmpty()) {
            return "No changes"
        }

        val added = added()
        val removed = removed()

        val parts = mutableListOf<String>()

        if (added.isNotEmpty()) {
            parts.add("Added: <b>${added.size}</b>")
        }

        if (removed.isNotEmpty()) {
            parts.add("Removed: <b>${removed.size}</b>")
        }

        if (parts.size == 1) {
            return "<br>" + parts.first()
        }

        return "<br>" + parts.mapIndexed { index, s ->
            if (index == 1) {
                s.lowercase()
            } else {
                s
            }
        }.joinToString(", ")
    }

    fun isEmpty() = symbols.isEmpty()

    companion object {
        private val LOG = logger<ModuliteDepsDiff>()
    }
}

data class ModuliteDeps(
    private var project: Project,
    private var modulite: Modulite?,
    val symbols: List<SymbolName> = emptyList(),
) {
    constructor(project: Project, modulite: Modulite?, name: SymbolName) : this(project, modulite, listOf(name))

    fun diff(other: ModuliteDeps): ModuliteDepsDiff {
        val diff = mutableMapOf<SymbolName, Int>()

        other.symbols.forEach { name ->
            if (!symbols.contains(name)) {
                diff[name] = -1 // символ был удален
            }
        }

        symbols.forEach { name ->
            if (!other.symbols.contains(name)) {
                diff[name] = 1 // символ был добавлен
            }
        }

        return ModuliteDepsDiff(project, modulite, diff)
    }

    fun union(other: ModuliteDeps) = ModuliteDeps(project, modulite, symbols + other.symbols)

    fun forModulite(other: Modulite): ModuliteDeps {
        modulite = other
        return this
    }

    fun isEmpty() = symbols.isEmpty()
}

@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
class ModuliteDependenciesCollector(val project: Project) {
    companion object {
        private val LOG = logger<ModuliteDependenciesCollector>()

        fun getInstance(project: Project) = project.service<ModuliteDependenciesCollector>()

        fun isInnerModulite(
            file: VirtualFile,
            moduliteConfig: VirtualFile?,
        ): Boolean {
            if (!file.isDirectory) {
                return false
            }

            val innerModuliteConfig = file.findChild(".modulite.yaml")
            if (innerModuliteConfig != null && innerModuliteConfig != moduliteConfig) {
                // Not iterate over nested modulites.
                return true
            }
            return false
        }
    }

    fun resolveNamespaceInside(dir: VirtualFile): Namespace {
        val namespaces = mutableListOf<Namespace>()

        dir.forEachFiles(project) { file ->
            if (file.isDirectory) return@forEachFiles true
            val psiFile = file.psiFile<PhpFile>(project) ?: return@forEachFiles true

            psiFile.accept(object : PhpRecursiveElementVisitor() {
                override fun visitPhpNamespace(ns: PhpNamespace) {
                    namespaces.add(Namespace(ns.fqn))
                }
            })

            true
        }

        return findCommonNamespace(namespaces)
    }

    fun findCommonNamespace(namespaces: List<Namespace>): Namespace {
        if (namespaces.isEmpty()) {
            return Namespace()
        }

        return Namespace(namespaces
            .map { it.toString().split("\\") }
            .reduce { acc, parts ->
                acc.zip(parts).mapNotNull { (a, b) -> if (a == b) a else null }
            }
            .joinToString("\\")
        )
    }
    data class TraitData(
        val traitsClasses: MutableSet<PhpClass>,
        val requireMethods: MutableSet<MethodImpl>,
        val requireConstants: MutableSet<PhpDefineImpl>,
        val functionsToRequire: MutableSet<FunctionImpl>,
    )

    //  тут запускается наш сборщик
    fun collect(
        dir: VirtualFile,
        ownSymbols: List<SymbolName>? = null,
        collapseModuleSymbols: Boolean = true,
    ): ModuliteDeps {
        val ownSymbolsSet = ownSymbols?.toSet() ?: ModuliteSymbolsCollector.getInstance(project).collect(dir).toSet()

        val symbols = mutableSetOf<SymbolName>()

        val modulites = ModuliteIndex.getInstance(project).getModulites()
        val composerPackages = ComposerPackagesIndex.getInstance(project).getPackages()

        val moduliteConfig = dir.findChild(".modulite.yaml")

        dir.forEachFilesEx(project) { file ->
            // Проверяем не отменил ли пользователь операцию.
            ProgressManager.checkCanceled()

            if (isInnerModulite(file, moduliteConfig)) {
                return@forEachFilesEx TreeNodeProcessingResult.SKIP_CHILDREN
            }

            val psiFile = file.psiFile<PhpFile>(project) ?: return@forEachFilesEx TreeNodeProcessingResult.CONTINUE

            psiFile.accept(object : PhpRecursiveElementVisitor() {
                override fun visitPhpClassReference(reference: ClassReference) {
                    when (reference.context) {
                        is PhpUse -> {
                            val useInstance = reference.context as PhpUseImpl
                            if (useInstance.isTraitImport) {
                                handleTraitReference(reference)
                            } else {
                                return
                            }
                        }

                        is MethodReference, is ClassConstantReference -> {
                            return
                        }
                    }

                     handleReference(reference)
                }

                override fun visitPhpFunctionCall(reference: FunctionReference) {
                    handleReference(reference)
                }

                override fun visitPhpMethodReference(reference: MethodReference) {
                    handleReference(reference)
                }

                override fun visitPhpNewExpression(expression: NewExpression) {
                    handleReference(expression.classReference)
                    super.visitPhpElement(expression)
                }

                override fun visitPhpFieldReference(reference: FieldReference) {
                    handleReference(reference)
                }

                override fun visitPhpGlobal(globalStatement: Global) {
                    globalStatement.variables.forEach { variable ->
                        handleReference(variable, traverseFurther = false)
                    }
                }

                override fun visitPhpConstantReference(reference: ConstantReference) {
                    handleReference(reference)
                }

                override fun visitPhpClassConstantReference(reference: ClassConstantReference) {
                    handleReference(reference)
                }

                fun getPhpDocTypes(type: PsiElement): List<PhpDocType> {
                    val types = mutableListOf<PhpDocType>()

                    if (type.children.isEmpty()) {
                        types.add(type as PhpDocType)
                    }

                    type.children.forEach {
                        if (it is PhpDocType) {
                            if (it.children.isEmpty() || it.firstChild is PhpNamespaceReference) {
                                types.add(it)
                            } else {
                                types.addAll(getPhpDocTypes(it))
                            }
                        }
                    }

                    return types
                }

                override fun visitPhpDocType(type: PhpDocType) {
                    getPhpDocTypes(type).forEach {
                        handleReference(it)
                    }
                }

                private fun referenceValidator(reference: PhpReference?): MutableCollection<out PhpNamedElement>? {
                    if (reference == null)
                        return null

                    val references = reference.resolveGlobal(false)
                    if (references.isEmpty()) {
                        LOG.warn("Неизвестная ссылка '${reference.safeFqn()}'")
                        return null
                    }

                    return references
                }

                private fun collectTraitElements(element: PsiElement): TraitData {
                    val classesToRequire: MutableSet<PhpClass> = mutableSetOf()
                    val methodsToRequire: MutableSet<MethodImpl> = mutableSetOf()
                    val constantsToRequire: MutableSet<PhpDefineImpl> = mutableSetOf()
                    val functionsToRequire: MutableSet<FunctionImpl> = mutableSetOf()

                    var child = element.firstChild

                    while (child != null) {
                        when (child) {
                            is Method -> methodsToRequire.add(child as MethodImpl)
                            is PhpClass -> classesToRequire.add(child)

                            is MethodReference -> {
                                val resolvedMethod = child.resolve()
                                if (resolvedMethod is Method) {
                                    methodsToRequire.add(resolvedMethod as MethodImpl)
                                    processParameters(resolvedMethod.parameters, classesToRequire)
                                }
                            }

                            is ConstantReference -> {
                                val resolvedConstant = child.resolve()
                                if (resolvedConstant is PhpDefineImpl) {
                                    constantsToRequire.add(resolvedConstant)
                                }
                            }

                            is NewExpression -> {
                                val classReference = child.classReference
                                val resolvedClass = classReference?.resolve()
                                if (resolvedClass is PhpClass) {
                                    classesToRequire.add(resolvedClass)
                                }
                                processArguments(
                                    child.parameters,
                                    classesToRequire,
                                    methodsToRequire,
                                    constantsToRequire,
                                    functionsToRequire
                                )
                            }

                            is FunctionReference -> {
                                val resolvedFunction = child.resolve()
                                if (resolvedFunction is FunctionImpl) {
                                    functionsToRequire.add(resolvedFunction)
                                    processParameters(resolvedFunction.parameters, classesToRequire)
                                }
                            }

                            is ParameterList -> {
                                processArguments(
                                    child.parameters,
                                    classesToRequire,
                                    methodsToRequire,
                                    constantsToRequire,
                                    functionsToRequire
                                )
                            }

                            else -> {
                                val (nestedClasses, nestedMethods, nestedConstants, nestedFunctions) = collectTraitElements(
                                    child
                                )
                                classesToRequire.addAll(nestedClasses)
                                methodsToRequire.addAll(nestedMethods)
                                constantsToRequire.addAll(nestedConstants)
                                functionsToRequire.addAll(nestedFunctions)
                            }
                        }

                        child = child.nextSibling
                    }

                    return TraitData(classesToRequire, methodsToRequire, constantsToRequire, functionsToRequire)
                }

                private fun processParameters(parameters: Array<Parameter>, classesToRequire: MutableSet<PhpClass>) {
                    parameters.forEach { parameter ->
                        if (!PhpType.isScalar(parameter.type, project)) {
                            var type = parameter.type.toString().substringAfterLast('\\')
                            if (type.endsWith("[]")) {
                                type = type.dropLast(2)
                            }
                            PhpIndex.getInstance(project).getClassByName(type)?.let { klass ->
                                classesToRequire.add(klass)
                            }
                        }
                    }
                }

                private fun processArguments(
                    arguments: Array<PsiElement>,
                    classesToRequire: MutableSet<PhpClass>,
                    methodsToRequire: MutableSet<MethodImpl>,
                    constantsToRequire: MutableSet<PhpDefineImpl>,
                    functionsToRequire: MutableSet<FunctionImpl>
                ) {
                    arguments.forEach { argument ->
                        when (argument) {
                            is FunctionReference -> {
                                val resolvedFunction = argument.resolve()
                                if (resolvedFunction is FunctionImpl) {
                                    functionsToRequire.add(resolvedFunction)
                                }
                            }
                            is NewExpression -> {
                                argument.classReference?.resolve()?.let { resolvedClass ->
                                    if (resolvedClass is PhpClass) {
                                        classesToRequire.add(resolvedClass)
                                    }
                                }
                                processArguments(
                                    argument.parameters,
                                    classesToRequire,
                                    methodsToRequire,
                                    constantsToRequire,
                                    functionsToRequire
                                )
                            }
                            else -> {
                                val (nestedClasses, nestedMethods, nestedConstants, nestedFunctions) = collectTraitElements(argument)
                                classesToRequire.addAll(nestedClasses)
                                methodsToRequire.addAll(nestedMethods)
                                constantsToRequire.addAll(nestedConstants)
                                functionsToRequire.addAll(nestedFunctions)
                            }
                        }
                    }
                }

                private fun handleTraitReference(reference: PhpReference?, traverseFurther: Boolean = true) {
                    val references = referenceValidator(reference) ?: return

                    val traitsClasses: MutableList<PhpClass> = arrayListOf()
                    val methodsNames: MutableCollection<Method> = arrayListOf()

                    val stack = LinkedList<PhpClass>() // Создаем стек для хранения вложенных instance

                    references.forEach { elem ->
                        val instance = elem as PhpClass
                        stack.push(instance) // Добавляем текущий instance в стек
                        while (stack.isNotEmpty()) {
                            val currentInstance = stack.pop() // Получаем текущий instance из стека
                            traitsClasses += currentInstance
                            if (currentInstance.hasTraitUses()) {
                                val traitsUses = currentInstance.traits
                                traitsClasses += traitsUses

                                traitsUses.forEach { it ->
                                    val instanceNesting: Array<PhpClass>? = it.traits

                                    instanceNesting?.forEach { nestedInstance ->
                                        stack.push(nestedInstance) // Добавляем вложенный instance в стек
                                    }

                                    if (instanceNesting != null) {
                                        traitsClasses += instanceNesting
                                    }
                                }
                            }
                        }
                        methodsNames += instance.methods
                    }

                    val requireMethods: MutableList<MethodImpl> = mutableListOf()
                    val requireConstants: MutableList<PhpDefineImpl> = mutableListOf()
                    val requireFunctions: MutableList<FunctionImpl> = mutableListOf()
                    methodsNames.forEach { it ->
                        val (classes, methods, constants, functions) = collectTraitElements(it)
                        traitsClasses.addAll(classes)
                        requireMethods.addAll(methods)
                        requireConstants.addAll(constants)
                        requireFunctions.addAll(functions)
                    }

                    val traitsClassName = traitsClasses.distinct().mapNotNull { processElement(it, reference) }
                    val traitsMethodsName = requireMethods.distinct().mapNotNull { processElement(it, reference) }
                    val traitsConstantsName = requireConstants.distinct().mapNotNull { processElement(it, reference) }
                    val traitsFunctionsName = requireFunctions.distinct().mapNotNull { processElement(it, reference) }

                    addSymbols(traitsClassName)
                    addSymbols(traitsMethodsName)
                    addSymbols(traitsConstantsName)
                    addSymbols(traitsFunctionsName)

                    if (traverseFurther) {
                        super.visitPhpElement(reference)
                    }
                }

                private fun handleReference(reference: PhpReference?, traverseFurther: Boolean = true) {
                    val references = referenceValidator(reference)?:return

                    val names = references.mapNotNull { processElement(it, reference) }
                    addSymbols(names)

                    if (traverseFurther) {
                        super.visitPhpElement(reference)
                    }
                }

                private fun processElement(element: PhpNamedElement, reference: PhpReference? = null): SymbolName? {
                    if (addReferenceModuleIfNeeded(element)) {
                        return null
                    }

                    if (element is MethodImpl) {
                        // У магических методов символ - его класс
                        if (PhpLangUtil.isMagicMethod(element.name)) {
                            val containingClass = element.containingClass
                            if (containingClass != null) {
                                return containingClass.symbolName(reference)
                            }
                        }

                        // Не статические методы добавлять не надо
                        if (!element.isStatic) {
                            return null
                        }
                    }

                    // Если у функции нет имени, то это лямбда. Значит мы её пропускаем
                    if (element is FunctionImpl) {
                        if (element.name.isEmpty()) {
                            return null
                        }
                    }

                    return element.symbolName(reference, forNotRequired = true)
                }

                private fun addSymbols(symbolList: List<SymbolName>) {
                    symbolList.forEach {
                        if (it.kind != SymbolName.Kind.Unknown) {
                            addSymbol(it)
                        }
                    }
                }

                private fun addSymbol(name: SymbolName) {
                    // TODO: simplify?
                    if (name.kind != SymbolName.Kind.Modulite && (ownSymbolsSet.contains(name) || ownSymbolsSet.contains(name.className()))) {
                        return
                    }

                    if (!symbols.contains(name)) {
                        symbols.add(name)
                    }
                }

                private fun addReferenceModuleIfNeeded(reference: PhpNamedElement): Boolean {
                    if (reference is Variable) {
                        // Глобальные переменные не могут содержаться в некотором модуле,
                        // они всегда считаются глобальными и обязательным для их использования
                        // является их запись в require модуля.
                        return false
                    }

                    if (reference is FieldImpl) {
                        // Не статические поля добавлять не надо
                        if (!reference.modifier.isStatic) {
                            return true
                        }
                    }

                    val containingPsiFile = reference.containingFile ?: return false
                    val containingFile = containingPsiFile.virtualFile ?: return false

                    // Если ссылка ссылается на символ из этих папок, то нам не
                    // нужно добавлять их в зависимости модуля, так как это не
                    // пользовательский код.
                    if (containingFile.fromStubs() || containingFile.fromKphpPolyfills() || containingFile.fromTests()) {
                        LOG.warn("${reference.fqn} from stubs/kphp-polyfill/tests")
                        return true
                    }

                    val modulite = containingFile.containingModulite(project, modulites)
                    if (modulite != null) {
                        addSymbol(modulite.symbolName())
                        return collapseModuleSymbols
                    }

                    val composerPackage = containingFile.containingComposerPackage(project, composerPackages)
                    if (composerPackage != null) {
                        addSymbol(composerPackage.symbolName())
                        return collapseModuleSymbols
                    }

                    return false
                }
            })

            TreeNodeProcessingResult.CONTINUE
        }

        return ModuliteDeps(project, null, symbols.toList())
    }
}
