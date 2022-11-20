package com.vk.modulite.utils

import com.intellij.ui.IconManager

object PluginIcons {
    private fun icon(name: String) = IconManager.getInstance().getIcon("/icons/$name", javaClass)

    val Modulite = icon("modulite.svg")
    val ModuliteInternalFolder = icon("internalFolder.svg")
    val ModuliteYamlFile = icon("moduliteYamlFile.svg")
    val MultiModulite = icon("multiModulite.svg")
    val InternalModulite = icon("internalModulite.svg")
    val InternalMultiModulite = icon("internalMultiModulite.svg")
}
