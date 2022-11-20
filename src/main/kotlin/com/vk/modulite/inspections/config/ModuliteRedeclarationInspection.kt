package com.vk.modulite.inspections.config

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils.insideName
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class ModuliteRedeclarationInspection : ConfigInspectionBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : YamlPsiElementVisitor() {
        override fun visitQuotedText(element: YAMLQuotedText) {
            if (!insideName(element)) {
                return
            }

            val name = element.text.unquote()
            val currentModulite = ModuliteIndex.getInstance(element.project).getModulite(name) ?: return

            val modulites = ModuliteIndex.getInstance(element.project).getModulites()
            val sameModulites = modulites.filter {
                it.name == name && it.containingPackage == currentModulite.containingPackage
            }

            if (sameModulites.size > 1) {
                holder.registerModuliteProblem(
                    element,
                    "Redeclaration of $name",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
    }
}
