package com.vk.modulite.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.vk.modulite.modulite.ModuliteDependenciesManager

class RegenerateModuleRequiresAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)

        ModuliteDependenciesManager.regenerate(project, file)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file == null || file.name != ".modulite.yaml") {
            e.presentation.isEnabledAndVisible = false
            return
        }
    }
}
