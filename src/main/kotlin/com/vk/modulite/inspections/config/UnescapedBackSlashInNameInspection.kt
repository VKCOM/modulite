package com.vk.modulite.inspections.config

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class UnescapedBackSlashInNameInspection : ConfigInspectionBase() {
    class NotEscapedBackSlashQuickFix(private val currentName: String) : LocalQuickFix {
        override fun getFamilyName() = "Escape backslashes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val el = descriptor.psiElement
            val newContent = currentName
                .replace("\\\\", "/")
                .replace("\\", "\\\\")
                .replace("/", "\\\\")

            el.replace(YamlUtils.createQuotedText(project, newContent))
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                val text = element.text.unquote()
                checkText(text, element)
            }

            override fun visitKeyValue(keyValue: YAMLKeyValue) {
                if (!YamlUtils.insideAllowInternalAccess(keyValue)) return
                val key = keyValue.key ?: return
                val text = key.text.unquote()
                checkText(text, key)
            }

            private fun checkText(text: String, element: PsiElement) {
                val replacedText = text.replace("\\\\", "/")
                if (replacedText.contains("\\")) {
                    holder.registerModuliteProblem(
                        element,
                        "Name must not contain unescaped backslashes",
                        ProblemHighlightType.GENERIC_ERROR,
                        NotEscapedBackSlashQuickFix(text)
                    )
                }
            }
        }
    }
}
