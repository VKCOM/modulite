package com.vk.modulite.inspections.config

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.SymbolName
import com.vk.modulite.completion.ModuliteYamlReferenceContributor.Companion.references
import com.vk.modulite.completion.ModuliteYamlReferenceContributor.PhpUnknownElementReference
import com.vk.modulite.psi.extensions.yaml.containingModulite
import com.vk.modulite.utils.YamlUtils.insideDescription
import com.vk.modulite.utils.YamlUtils.insideName
import com.vk.modulite.utils.YamlUtils.insideNamespace
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YAMLScalarText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class UnknownSymbolInspection : ConfigInspectionBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitScalarText(element: YAMLScalarText) {
                handleText(element.text, element)
            }

            override fun visitQuotedText(element: YAMLQuotedText) {
                handleText(element.text.unquote(), element)
            }

            private fun handleText(rawName: String, element: YAMLPsiElement) {
                if (rawName.startsWith("@") || rawName.startsWith("#") || rawName.startsWith("$") || rawName.isEmpty()) {
                    return
                }

                if (insideNamespace(element) || insideName(element) || insideDescription(element)) {
                    return
                }

                val name = SymbolName(rawName)

                val isUnknown = element.references().all { it is PhpUnknownElementReference }
                if (isUnknown) {
                    val namespace = element.containingModulite()?.namespace

                    val fqn = if (namespace != null) {
                        name.absolutize(namespace)
                    } else {
                        name
                    }

                    val readableName = fqn.readableName(uppercase = false)

                    holder.registerModuliteProblem(
                        element,
                        "Unknown $readableName",
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }
        }
    }
}
