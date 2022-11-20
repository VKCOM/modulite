package com.vk.modulite.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class NewModuliteFromFolderAction : AnAction(
    "Modulite from Folder...",
    "Create a modulite from existing folder",
    AllIcons.Nodes.Module
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val folder = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        ModuliteBuilder(project).startBuild(folder, fromSource = true)
    }

    override fun update(e: AnActionEvent) {
        if (e.project == null) {
            e.presentation.isEnabled = false
            return
        }

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (!virtualFile.isDirectory) {
            e.presentation.isEnabled = false
            return
        }

        val moduliteConfig = virtualFile.findChild(".modulite.yaml")
        if (moduliteConfig != null) {
            e.presentation.isEnabled = false
            return
        }
    }
}
