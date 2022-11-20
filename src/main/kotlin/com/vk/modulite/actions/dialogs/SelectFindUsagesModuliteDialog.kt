package com.vk.modulite.actions.dialogs

import com.intellij.openapi.project.Project
import com.vk.modulite.modulite.Modulite

class SelectFindUsagesModuliteDialog(
    project: Project,
    currentModulite: Modulite?,
) : SelectModuliteDialogBase(
    project,
    currentModulite,
    "Select a modulite to find usages:",
) {
    companion object {
        fun request(
            project: Project,
            currentModulite: Modulite?,
        ): Modulite? {
            val dialog = SelectFindUsagesModuliteDialog(project, currentModulite)
            if (!dialog.showAndGet()) {
                return null
            }
            return dialog.selected()
        }
    }

    init {
        init()
    }
}
