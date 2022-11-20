package com.vk.modulite.inspections.config

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.completion.ModuliteYamlReferenceContributor.Companion.references
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.utils.createModuliteProblemDescriptor
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YAMLScalarText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class UnknownModuliteInspection : ConfigInspectionBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitScalarText(element: YAMLScalarText) {
                handleName(element.text, element)
            }

            override fun visitQuotedText(element: YAMLQuotedText) {
                handleName(element.text.unquote(), element)
            }

            private fun handleName(name: String, element: YAMLPsiElement) {
                if (!name.startsWith("@")) {
                    return
                }

                val refs = element.references()

                refs.filterIsInstance<ModuliteNamePsi.UnknownModuleReference>().forEach {
                    holder.registerProblem(
                        InspectionManager.getInstance(element.project)
                            .createModuliteProblemDescriptor(
                                element,
                                it.rangeInElement,
                                "Unknown modulite ${it.name}",
                                ProblemHighlightType.GENERIC_ERROR,
                                isOnTheFly,
                            )
                    )
                }
            }
        }
    }
}
