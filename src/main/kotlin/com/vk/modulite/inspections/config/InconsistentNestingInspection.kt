package com.vk.modulite.inspections.config

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.registerModuliteProblem
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class InconsistentNestingInspection : ConfigInspectionBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : YamlPsiElementVisitor() {
            override fun visitQuotedText(element: YAMLQuotedText) {
                if (!YamlUtils.insideName(element)) {
                    return
                }

                val name = element.text.unquote()
                val indices = name.indices.filter { name[it] == '/' }

                val names = mutableListOf<String>()
                names.add(name)
                for (i in indices.size - 1 downTo 0) {
                    names.add(name.substring(0, indices[i]))
                }

                val currentModulite = element.containingModulite() ?: return
                var current: Modulite? = currentModulite
                for (parentName in names) {
                    if (current == null) {
                        holder.registerModuliteProblem(
                            element,
                            "Inconsistent nesting: $name outside of $parentName",
                            ProblemHighlightType.GENERIC_ERROR,
                        )
                        return
                    }

                    if (current.name != parentName) {
                        holder.registerModuliteProblem(
                            element,
                            "Inconsistent nesting: $name placed in ${current.name}",
                            ProblemHighlightType.GENERIC_ERROR,
                        )
                        return
                    }

                    current = current.actualParent()
                }

                if (current != null) {
                    holder.registerModuliteProblem(
                        element,
                        "Inconsistent nesting: $name placed in ${current.name}",
                        ProblemHighlightType.GENERIC_ERROR,
                        EmptyValueInListInspection.RemoveEmptyValueFromListQuickFix()
                    )
                }
            }
        }
    }
}
