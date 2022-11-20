package com.vk.modulite.actions.usages.php

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.util.SlowOperations
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.jetbrains.php.lang.psi.elements.Variable
import com.vk.modulite.actions.PhpPsiElementAction
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite

class FindSymbolUsagesInCurrentModuleAction : PhpPsiElementAction<PhpPsiElement>(PhpPsiElement::class.java) {
    override fun actionPerformed(element: PhpPsiElement) {
        PhpUsagesFinder().find(element)
    }

    override fun update(e: AnActionEvent, element: PhpPsiElement?) {
        if (element == null || element is Variable) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val modulite = SlowOperations.allowSlowOperations<Modulite?, RuntimeException> {
            e.getData(CommonDataKeys.VIRTUAL_FILE)?.containingModulite(e.project!!)
        }

        if (modulite == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Find Usages in ${modulite.name}"
    }

    override val errorHint: String = "Error"
}
