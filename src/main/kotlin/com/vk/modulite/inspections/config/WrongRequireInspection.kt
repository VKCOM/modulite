package com.vk.modulite.inspections.config

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.modulite.ModuliteRequireRestrictionChecker
import com.vk.modulite.modulite.ModuliteRequireRestrictionChecker.ViolationTypes
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class WrongRequireInspection : ConfigInspectionBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (!YamlUtils.insideRequires(element)) {
                    return
                }

                val elText = element.text.unquote()
                if (!elText.startsWith("@")) {
                    return
                }

                val currentModulite = element.containingModulite() ?: return
                val requiredModulite = ModuliteIndex.getInstance(element.project).getModulite(elText) ?: return

                val (canUse, reason) = ModuliteRequireRestrictionChecker.canUse(currentModulite, requiredModulite)
                if (!canUse) {
                    val text = when (reason) {
                        ViolationTypes.Ok -> return
                        ViolationTypes.RequireSelf -> {
                            "Can't require itself"
                        }
                        ViolationTypes.NotPublic -> {
                            val parentRequiredModulite = requiredModulite.parent() ?: return
                            "restricted to use $requiredModulite, it's internal in $parentRequiredModulite"
                        }
                    }

                    holder.registerModuliteProblem(
                        element,
                        text,
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }
        }
    }
}
