package com.vk.modulite.view

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.vk.modulite.settings.ModuliteSettings
import com.vk.modulite.utils.PluginIcons
import org.jetbrains.yaml.psi.YAMLFile
import javax.swing.Icon

class ModuliteIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        val settings = ModuliteSettings.getInstance()
        val iconTurnOff = settings.state.turnOffIconOnYaml
        if (iconTurnOff) {
            return null
        }

        if (element !is YAMLFile) {
            return null
        }

        if (element.name != ".modulite.yaml") {
            return null
        }

        return PluginIcons.ModuliteYamlFile
    }
}
