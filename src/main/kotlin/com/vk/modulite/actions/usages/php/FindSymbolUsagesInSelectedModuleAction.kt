package com.vk.modulite.actions.usages.php

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.jetbrains.php.lang.psi.elements.Variable
import com.vk.modulite.actions.PhpPsiElementAction
import com.vk.modulite.actions.dialogs.SelectFindUsagesModuliteDialog
import com.vk.modulite.psi.extensions.files.containingModulite

class FindSymbolUsagesInSelectedModuleAction : PhpPsiElementAction<PhpPsiElement>(PhpPsiElement::class.java) {
    override fun actionPerformed(element: PhpPsiElement) {
        val modulite = SelectFindUsagesModuliteDialog.request(element.project, element.containingModulite()) ?: return
        PhpUsagesFinder(modulite).find(element)
    }

    override fun update(e: AnActionEvent, element: PhpPsiElement?) {
        if (element == null || element is Variable) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Find Usages in Modulite..."
    }

    override val errorHint: String = "Error"
}
