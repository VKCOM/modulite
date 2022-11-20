package com.vk.modulite.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class NewModuliteAction : AnAction(
    "Modulite...",
    "Create a blank modulite",
    AllIcons.Nodes.Module
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val folder = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        ModuliteBuilder(project).startBuild(folder, fromSource = false)
    }
}
