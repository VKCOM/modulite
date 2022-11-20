package com.vk.modulite.inspections.config

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class InvalidNamespaceInspection : ConfigInspectionBase() {
    class InsertGlobalNSQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Insert \"\\\\\""

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val el = descriptor.psiElement
            val newContent = "\\\\"
            el.replace(YamlUtils.createQuotedText(project, newContent))
        }
    }

    class InsertTrailingSlashQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Insert \"\\\\\""

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val el = descriptor.psiElement
            val newContent = el.text.unquote() + "\\\\"
            el.replace(YamlUtils.createQuotedText(project, newContent))
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (!YamlUtils.insideNamespace(element)) {
                    return
                }

                val namespace = element.text.unquote()
                if (namespace.isEmpty()) {
                    holder.registerModuliteProblem(
                        element,
                        "Namespace must not be empty (use \\\\ for global scope)",
                        ProblemHighlightType.GENERIC_ERROR,
                        InsertGlobalNSQuickFix(),
                    )
                    return
                }

                if (!namespace.endsWith("\\\\")) {
                    holder.registerModuliteProblem(
                        element,
                        "Namespace must end with \\\\",
                        ProblemHighlightType.GENERIC_ERROR,
                        InsertTrailingSlashQuickFix(),
                    )
                }
            }
        }
    }
}
