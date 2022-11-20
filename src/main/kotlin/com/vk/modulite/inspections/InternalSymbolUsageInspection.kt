package com.vk.modulite.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.vk.modulite.SymbolName
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteRestrictionChecker
import com.vk.modulite.psi.extensions.files.containingComposerPackage
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.utils.fromStubs
import com.vk.modulite.utils.fromTests
import com.vk.modulite.utils.fromVendor
import com.vk.modulite.utils.registerModuliteProblem

class InternalSymbolUsageInspection : LocalInspectionTool() {
    companion object {
        private val LOG = logger<InternalSymbolUsageInspection>()
    }

    class AddSymbolToRequiresQuickFix(
        private val contextModulite: Modulite,
        private val symbol: SymbolName,
    ) : LocalQuickFix {
        override fun getFamilyName() = "Add symbol to requires"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            contextModulite.addDependencies(symbol)
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

            private fun checkReferenceUsage(reference: PhpReference, problemElement: PsiElement? = reference) {
                val references = reference.resolveGlobal(false)
                if (references.isEmpty()) {
//                    LOG.warn("Unknown reference for symbol '${reference.safeFqn()}'")
                    return
                }

                val filteredReferences = references.filter {
                    val file = it.containingFile.virtualFile
                    !file.fromTests() && !file.fromVendor() && !file.fromStubs() && it !is PhpNamespace
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
                } else {
                    quickFixes.add(AddSymbolToRequiresQuickFix(context.modulite!!, symbol))

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

        registerModuliteProblem(
            problemElement,
            text,
            ProblemHighlightType.GENERIC_ERROR,
            *quickFixes.toTypedArray()
        )
    }
}
