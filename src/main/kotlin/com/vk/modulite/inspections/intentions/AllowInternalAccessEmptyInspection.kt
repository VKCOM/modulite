package com.vk.modulite.inspections.intentions

import com.intellij.codeInspection.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiManager
import com.intellij.psi.util.parentOfType
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.vk.modulite.actions.dialogs.SelectAllowInternalModuliteDialog
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.allowElementForModulite
import org.jetbrains.yaml.psi.YAMLFile

class AllowInternalAccessEmptyInspection : LocalInspectionTool() {
    companion object {
        /**
         * Should be called from EDT
         */
        fun allowInternalAccess(
            project: Project,
            element: PhpNamedElement,
            currentModulite: Modulite,
            configFile: VirtualFile
        ) {
            val selectedModulite = SelectAllowInternalModuliteDialog.request(project, element, currentModulite)
                ?: return

            val elementModulitePsiFile = PsiManager.getInstance(project).findFile(configFile) ?: return

            val moduliteFile = elementModulitePsiFile as YAMLFile
            moduliteFile.allowElementForModulite(element, selectedModulite)
        }
    }

    class AllowInternalAccessQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Allow internal symbol for a specific modulite"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement?.parentOfType<PhpNamedElement>() ?: return
            val modulite = element.containingModulite() ?: return
            val configFile = modulite.configFile() ?: return

            invokeLater {
                allowInternalAccess(project, element, modulite, configFile)
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : BaseIntentionPhpVisitor() {
            override fun addProblem(namedElement: PhpNamedElement) {
                val namePsi = namedElement.nameIdentifier ?: return
                val module = namedElement.containingModulite() ?: return
                val isExported = module.isExport(namedElement)

                if (isExported) {
                    return
                }

                holder.registerProblem(
                    namePsi,
                    "Allow internal access",
                    ProblemHighlightType.INFORMATION,
                    AllowInternalAccessQuickFix(),
                )
            }
        }
    }
}
