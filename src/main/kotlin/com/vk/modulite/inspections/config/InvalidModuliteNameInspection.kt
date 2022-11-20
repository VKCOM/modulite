package com.vk.modulite.inspections.config

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class InvalidModuliteNameInspection : ConfigInspectionBase() {
    class ModuliteNameQuickFix(
        private val currentName: String,
        private val missingAt: Boolean = false,
        private val containSpaces: Boolean = false,
    ) : LocalQuickFix {
        override fun getFamilyName() = "Fix modulite name"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val el = descriptor.psiElement
            var newContent = currentName

            if (missingAt) {
                newContent = "@$currentName"
            }
            if (containSpaces) {
                newContent = newContent.trim().replace(" ", "-")
            }

            el.replace(YamlUtils.createQuotedText(project, newContent))
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (!YamlUtils.insideName(element)) {
                    return
                }

                val moduliteName = element.text.unquote()
                val missingAt = !moduliteName.startsWith("@")
                val containSpaces = moduliteName.contains(" ")

                if (missingAt) {
                    holder.registerModuliteProblem(
                        element,
                        "Modulite name must start with @",
                        ProblemHighlightType.GENERIC_ERROR,
                        ModuliteNameQuickFix(moduliteName, missingAt = true, containSpaces)
                    )
                }

                if (containSpaces) {
                    holder.registerModuliteProblem(
                        element,
                        "Modulite name can't contain spaces",
                        ProblemHighlightType.GENERIC_ERROR,
                        ModuliteNameQuickFix(moduliteName, missingAt, containSpaces = true)
                    )
                }
            }
        }
    }
}
