package com.vk.modulite.projectview

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.vk.modulite.settings.ModuliteSettings

class ProjectViewDecorator : ProjectViewNodeDecorator {
    override fun decorate(node: ProjectViewNode<*>, presentation: PresentationData) {
        val settings = ModuliteSettings.getInstance()
        val iconTurnOff = settings.state.turnOffIconsOnFolders

        if (!iconTurnOff) {
            presentation.isChanged = ModuliteNodeDecoration.apply(node, presentation)
        }
    }
}
