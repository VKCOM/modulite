package com.vk.modulite.inspections.config

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.psi.extensions.yaml.containingModulite
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.YamlUtils.createQuotedText
import com.vk.modulite.utils.createModuliteProblemDescriptor
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class UnnecessaryFullyQualifiedNameInspection : ConfigInspectionBase() {
    class QualifierQuickFix(private val prefix: String) : LocalQuickFix {
        override fun getFamilyName() = "Remove unnecessary qualifier"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val el = descriptor.psiElement
            val content = el.text.unquote()
            val newContent = content.removePrefix(prefix)
            el.replace(createQuotedText(project, newContent))
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (YamlUtils.insideNamespace(element) || YamlUtils.insideRequires(element)) {
                    return
                }

                val modulite = element.containingModulite() ?: return

                val name = element.text.unquote()
                val namespace = "\\\\" + modulite.namespace.toYaml()
                if (name.startsWith(namespace)) {
                    holder.registerProblem(
                        InspectionManager.getInstance(element.project)
                            .createModuliteProblemDescriptor(
                                element,
                                TextRange(1, namespace.length + 1),
                                "Qualifier is unnecessary and can be removed",
                                ProblemHighlightType.WEAK_WARNING,
                                isOnTheFly,
                                QualifierQuickFix(namespace)
                            )
                    )
                }
            }
        }
    }
}
