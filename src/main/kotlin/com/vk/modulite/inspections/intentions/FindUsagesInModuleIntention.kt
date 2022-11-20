package com.vk.modulite.inspections.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.vk.modulite.actions.usages.php.PhpUsagesFinder
import com.vk.modulite.psi.extensions.files.containingModulite

class FindUsagesInModuleIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "Find usages in the current modulite"
    override fun getText() = "Find usages in the current modulite"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.containingModulite() != null && element.parent is PhpPsiElement
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) = invokeLater {
        PhpUsagesFinder().find(element)
    }
}
