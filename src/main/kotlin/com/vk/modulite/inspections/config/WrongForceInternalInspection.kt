package com.vk.modulite.inspections.config

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.psi.extensions.yaml.resolveSymbolName
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.registerModuliteProblem
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class WrongForceInternalInspection : ConfigInspectionBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (!YamlUtils.insideForceInternal(element)) {
                    return
                }

                val name = element.resolveSymbolName() ?: return

                if (!name.isClassMember()) {
                    holder.registerModuliteProblem(
                        element,
                        "Force internal is only allowed for class members",
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }
        }
    }
}
