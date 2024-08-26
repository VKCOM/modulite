package com.vk.modulite.projectview

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.vk.modulite.settings.ModuliteSettingsState

class ProjectViewDecorator : ProjectViewNodeDecorator {
    override fun decorate(node: ProjectViewNode<*>, presentation: PresentationData) {
        val settings = ModuliteSettingsState.getInstance()
        val iconTurnOff = settings.turnOffIconsOnFolders

        if (!iconTurnOff) {
            presentation.isChanged = ModuliteNodeDecoration.apply(node, presentation)
        }
    }
}
