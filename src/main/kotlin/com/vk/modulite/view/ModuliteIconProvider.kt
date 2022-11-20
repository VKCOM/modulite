package com.vk.modulite.view

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.vk.modulite.utils.PluginIcons
import org.jetbrains.yaml.psi.YAMLFile
import javax.swing.Icon

class ModuliteIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element !is YAMLFile) {
            return null
        }

        if (element.name != ".modulite.yaml") {
            return null
        }

        return PluginIcons.ModuliteYamlFile
    }
}
