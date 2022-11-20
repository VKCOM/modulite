package com.vk.modulite.inspections.intentions

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.makeElementExport
import com.vk.modulite.psi.extensions.yaml.makeElementInternal

class ChangeVisibilityEmptyInspection : LocalInspectionTool() {
    class ChangeVisibilityQuickFix(
        private val moduliteName: String,
        private val isExported: Boolean,
    ) : LocalQuickFix {
        override fun getFamilyName() = (if (isExported) "Make internal in " else "Make exported from ") + moduliteName

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val modulite = descriptor.psiElement.containingModulite() ?: return
            val configFile = modulite.configPsiFile() ?: return
            val element = descriptor.psiElement?.parentOfType<PhpNamedElement>() ?: return
            if (!isExported) {
                configFile.makeElementExport(element)
            } else {
                configFile.makeElementInternal(element)
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : BaseIntentionPhpVisitor() {
            override fun addProblem(namedElement: PhpNamedElement) {
                val namePsi = namedElement.nameIdentifier ?: return
                val modulite = namedElement.containingModulite() ?: return
                val isPublic = modulite.isExport(namedElement)

                if (namedElement is Method || namedElement is Field) {
                    val klass = PsiTreeUtil.findFirstParent(namedElement) { it is PhpClass } as PhpClass?
                    val klassIsPublic = klass != null && modulite.isExport(klass)

                    if (!klassIsPublic && !isPublic) {
                        // if class is not public and method is not public,
                        // we don't need to show the quick intention Make Public
                        return
                    }
                }

                holder.registerProblem(
                    namePsi,
                    "Change visibility",
                    ProblemHighlightType.INFORMATION,
                    ChangeVisibilityQuickFix(modulite.name, isPublic),
                )
            }
        }
    }
}
