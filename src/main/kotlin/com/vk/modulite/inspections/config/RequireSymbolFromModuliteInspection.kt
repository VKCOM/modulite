package com.vk.modulite.inspections.config

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.vk.modulite.modulite.ModuliteDependenciesManager
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.psi.extensions.yaml.containingModulite
import com.vk.modulite.psi.extensions.yaml.resolveSymbol
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils.insideRequires
import com.vk.modulite.utils.registerModuliteProblem
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class RequireSymbolFromModuliteInspection : ConfigInspectionBase() {
    class OptimizeRequiresQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Optimize requires"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val modulite = descriptor.psiElement.containingModulite() ?: return
            val configFile = modulite.configFile() ?: return
            ModuliteDependenciesManager.regenerate(project, configFile)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (!insideRequires(element)) {
                    return
                }

                val currentModulite = element.containingModulite() ?: return

                val refs = element.resolveSymbol()
                val ref = refs.firstOrNull() as? PhpNamedElement ?: return

                val modulites = ModuliteIndex.getInstance(element.project).getModulites()
                val moduliteWithSymbol = modulites.find {
                    it.contains(ref)
                } ?: return

                val alreadyRequired = currentModulite.requires.modulites().any {
                    it.name == moduliteWithSymbol.name
                }

                val quickFixes = mutableListOf<LocalQuickFix>()
                val description = if (alreadyRequired) {
                    quickFixes.add(OptimizeRequiresQuickFix())

                    """
                        Current modulite already requires ${moduliteWithSymbol.name},
                        there is no need to explicitly specify its symbols
                    """.trimIndent()
                } else if (moduliteWithSymbol == currentModulite) {
                    quickFixes.add(OptimizeRequiresQuickFix())

                    """
                        Current modulite requires a symbol from itself, there is no need to explicitly
                        specify the ${moduliteWithSymbol.name} symbols
                    """.trimIndent()
                } else {
                    quickFixes.add(OptimizeRequiresQuickFix())

                    val readableName = ref.symbolName().readableName()
                    val readableReversedName = ref.symbolName().readableName(uppercase = false, reversed = true)

                    """
                        $readableName is a part of ${moduliteWithSymbol.name},
                        require ${moduliteWithSymbol.name} directly instead of $readableReversedName
                    """.trimIndent()
                }

                holder.registerModuliteProblem(
                    element,
                    description,
                    ProblemHighlightType.GENERIC_ERROR,
                    *quickFixes.toTypedArray()
                )
            }
        }
    }
}
