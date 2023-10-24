package com.vk.modulite.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpUseImpl
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.vk.modulite.SymbolName
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteRequires
import com.vk.modulite.modulite.ModuliteRestrictionChecker
import com.vk.modulite.psi.extensions.files.containingComposerPackage
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.utils.fromStubs
import com.vk.modulite.utils.fromTests
import com.vk.modulite.utils.registerModuliteProblem
import java.util.*

class InternalSymbolUsageInspection : LocalInspectionTool() {
    companion object {
        private val LOG = logger<InternalSymbolUsageInspection>()
    }

    class AddSymbolToRequiresQuickFix(
        private val contextModulite: Modulite,
        private val symbols: List<SymbolName>
    ) : LocalQuickFix {

        override fun getFamilyName() = "Add symbol to requires"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            contextModulite.addDependencies(symbols)
        }
    }

    class AddModuliteToRequiresQuickFix(
        private val contextModulite: Modulite,
        private val referenceModulite: Modulite,
    ) : LocalQuickFix {

        override fun getFamilyName() = "Add $referenceModulite to requires"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            contextModulite.addDependencies(referenceModulite.symbolName())
            return
        }
    }
    private fun referenceValidator(reference: PhpReference?): MutableCollection<out PhpNamedElement>? {
        if (reference == null) return null

        val references = reference.resolveGlobal(false)
        if (references.isEmpty()) {
//                    LOG.warn("Unknown reference for symbol '${reference.safeFqn()}'")
            return null
        }

        return references
    }
    class AddComposerPackageToRequiresQuickFix(
        private val contextModulite: Modulite,
        private val referencePackage: ComposerPackage,
    ) : LocalQuickFix {

        override fun getFamilyName() = "Add composer package $referencePackage to requires"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            contextModulite.addDependencies(referencePackage.symbolName())
            return
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFunctionCall(reference: FunctionReference) {
                if (reference.parent is PhpUse) {
                    return
                }

                checkReferenceUsage(reference, reference.firstChild)
            }

            override fun visitPhpMethodReference(reference: MethodReference) {
                val methodNamePsi = reference.firstChild?.nextSibling?.nextSibling

                if (!reference.isStatic) {
                    // Don't check non-static method references
                    return
                }

                checkReferenceUsage(reference, methodNamePsi)
            }

            override fun visitPhpFieldReference(reference: FieldReference) {
                if (!reference.isStatic) {
                    // Don't check non-static field references
                    return
                }

                checkReferenceUsage(reference)
            }

            override fun visitPhpConstantReference(reference: ConstantReference) {
                if (reference.parent is PhpUse) {
                    return
                }

                checkReferenceUsage(reference)
            }

            override fun visitPhpClassConstantReference(reference: ClassConstantReference) {
//                val identifier = reference.lastChild
                checkReferenceUsage(reference)
            }

            override fun visitPhpClassReference(reference: ClassReference) {
                if (reference.parent is PhpUse) {
                    return
                }

                checkReferenceUsage(reference)
            }

            override fun visitPhpGlobal(globalStatement: Global) {
                globalStatement.variables.forEach {
                    checkReferenceUsage(it)
                }
            }

            override fun visitPhpDocType(type: PhpDocType) {
                checkReferenceUsage(type)
            }

            override fun visitPhpUse(expression: PhpUse?) {
                val instance = expression as PhpUseImpl

                if (instance.isTraitImport) {
                   instance.targetReference?.let { checkReferenceUsage(it) }
                }
            }

            private fun checkReferenceUsage(reference: PhpReference, problemElement: PsiElement? = reference) {
                val references = referenceValidator(reference) ?: return

                val filteredReferences = references.filter {
                    val file = it.containingFile.virtualFile
                    !file.fromTests() && !file.fromStubs() && it !is PhpNamespace
                }

                val problemPsiElement = problemElement ?: reference
                val context = ModuliteRestrictionChecker.createContext(reference)

                filteredReferences.forEach { element ->
                    val (can, reason) = ModuliteRestrictionChecker.canUse(context, element, reference)
                    if (!can) {
                        holder.addProblem(
                            reason,
                            element,
                            reference,
                            context,
                            problemPsiElement
                        )
                    }
                }

            }
        }
    }

    private fun processElement(element: PhpNamedElement, reference: PhpReference? = null): SymbolName? {

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

    private fun ProblemsHolder.addProblem(
        reason: ModuliteRestrictionChecker.ViolationTypes,
        symbolElement: PhpNamedElement,
        reference: PhpReference,
        context: ModuliteRestrictionChecker.Context,
        problemElement: PsiElement,
    ) {
        val refModulite = symbolElement.containingModulite()
        val refPackage = symbolElement.containingComposerPackage()

        val quickFixes = mutableListOf<LocalQuickFix>()
        val text = when (reason) {
            ModuliteRestrictionChecker.ViolationTypes.Ok                    -> return

            ModuliteRestrictionChecker.ViolationTypes.NotPublic             -> {
                if (refModulite == null) return
                val readableName = symbolElement.symbolName(forNotPublic = true).readableNameWithAction()

                "restricted to $readableName, it's internal in $refModulite"
            }

            ModuliteRestrictionChecker.ViolationTypes.NotRequired           -> {
                val symbol = symbolElement.symbolName(reference, forNotRequired = true)
                val readableName = symbol.readableNameWithAction()

                // Если символ определен в композер пакете, то нужно добавить его, а не модуль.
                if (refPackage != null) {
                    quickFixes.add(AddComposerPackageToRequiresQuickFix(context.modulite!!, refPackage))

                    """
                        restricted to $readableName, $refPackage it not required by ${context.modulite}
                    """.trimIndent()
                } else if (refModulite != null) {
                    quickFixes.add(AddModuliteToRequiresQuickFix(context.modulite!!, refModulite))

                    """
                        restricted to $readableName, $refModulite is not required by ${context.modulite}
                    """.trimIndent()
                } else if(symbolElement is PhpClass && symbolElement.isTrait){
                    val (classes,methods) = collectTraitReferenceUsage(reference,context.modulite?.requires )
                    quickFixes.add(AddSymbolToRequiresQuickFix(context.modulite!!, classes+methods))

                    """
                        restricted to $readableName, it's not required by ${context.modulite}
                    """.trimIndent()
                }
                    else {
                    quickFixes.add(AddSymbolToRequiresQuickFix(context.modulite!!, listOf(symbol)))

                    """
                        restricted to $readableName, it's not required by ${context.modulite}
                    """.trimIndent()
                }
            }

            ModuliteRestrictionChecker.ViolationTypes.CantUseNestedModulite -> {
                val readableName = symbolElement.symbolName().readableNameWithAction()

                """
                    restricted to $readableName, it belongs to ${refModulite?.name},
                    which is internal in its parent modulite
                """.trimIndent()
            }
        }
context.modulite?.requires
        registerModuliteProblem(
            problemElement,
            text,
            ProblemHighlightType.GENERIC_ERROR,
            *quickFixes.toTypedArray()
        )
    }

    private fun collectElements(element: PsiElement): Pair<MutableList<PhpClass>, MutableList<MethodImpl>> {
        val classesToRequire: MutableList<PhpClass> = mutableListOf()
        val methodsToRequire: MutableList<MethodImpl> = mutableListOf()

        var child = element.firstChild

        while (child != null) {
            when (child) {
                is Method -> methodsToRequire.add(child as MethodImpl)
                is PhpClass -> classesToRequire.add(child)
                is MethodReference -> {
// Извлечь информацию о вызываемом методе
                    val resolvedMethod = child.resolve()
                    if (resolvedMethod is Method) {
                        methodsToRequire.add(resolvedMethod as MethodImpl)
                    }
                }

                is NewExpression -> {
                    val classReference = child.classReference
                    val resolvedClass = classReference?.resolve()
                    if (resolvedClass is PhpClass) {
                        classesToRequire.add(resolvedClass)
                    }
                }

                else -> {
// Рекурсивный вызов для дочерних элементов
                    val (nestedClasses, nestedMethods) = collectElements(child)
                    classesToRequire.addAll(nestedClasses)
                    methodsToRequire.addAll(nestedMethods)
                }
            }

            child = child.nextSibling
        }

        return Pair(classesToRequire, methodsToRequire)
    }

    private fun collectTraitReferenceUsage(reference: PhpReference, moduliteRequires : ModuliteRequires? = null)
            : Pair<List<SymbolName>, List<SymbolName>> {
        val traitsClasses: MutableList<PhpClass> = arrayListOf()
        val methodsNames: MutableCollection<Method> = arrayListOf()

        val references = referenceValidator(reference) ?: return Pair(listOf<SymbolName>(), listOf<SymbolName>())

        val filteredReferences = references.filter {
            val file = it.containingFile.virtualFile
            !file.fromTests() && !file.fromStubs() && it !is PhpNamespace
        }

        val stack = LinkedList<PhpClass>() // Создаем стек для хранения вложенных instance

        filteredReferences.forEach { elem ->
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
        methodsNames.forEach { it ->
            val (classes, methods) = collectElements(it)
            traitsClasses.addAll(classes)
            requireMethods.addAll(methods)
        }

        val traitsClassName = traitsClasses.distinct().mapNotNull { processElement(it, reference) }
        val traitsMethodsName = requireMethods.distinct().mapNotNull { processElement(it, reference) }

        moduliteRequires?.symbols?.let { currentModuleSymbols ->
            val filteredMethods = traitsMethodsName.filterNot { it in currentModuleSymbols }
            val filteredClasses = traitsClassName.filterNot { it in currentModuleSymbols }
            return Pair(filteredClasses, filteredMethods)
        }

        return Pair(traitsClassName, traitsMethodsName)
    }
}
