package com.vk.modulite.inspections.config

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class EmptyValueInListInspection : ConfigInspectionBase() {
    class RemoveEmptyValueFromListQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Remove empty value"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val el = descriptor.psiElement
            if (el.parent != null && el.parent is YAMLSequenceItem) {
                el.parent.delete()
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                val inSequence = element.parent != null && element.parent is YAMLSequenceItem
                val text = element.text.unquote()

                if (text.isEmpty() && inSequence) {
                    holder.registerModuliteProblem(
                        element,
                        "Strange empty value in list",
                        ProblemHighlightType.WEAK_WARNING,
                        RemoveEmptyValueFromListQuickFix()
                    )
                }
            }
        }
    }
}
