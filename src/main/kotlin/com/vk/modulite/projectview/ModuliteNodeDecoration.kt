package com.vk.modulite.projectview

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.PresentableNodeDescriptor.ColoredFragment
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.FontUtil.spaceAndThinSpace
import com.jetbrains.php.lang.psi.PhpFile
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.utils.PluginIcons

object ModuliteNodeDecoration {
    private val wideSpacer = ColoredFragment(spaceAndThinSpace(), SimpleTextAttributes.REGULAR_ATTRIBUTES)

    fun apply(node: ProjectViewNode<*>, data: PresentationData): Boolean {
        setName(data)

        if (node.virtualFile == null) {
            return false
        }

        if (node.virtualFile is VirtualDirectoryImpl) {
            val dir = node.virtualFile as VirtualDirectoryImpl
            val moduleConfigFile = dir.findChild(".modulite.yaml")

            if (moduleConfigFile != null) {
                val modulite = moduleConfigFile.containingModulite(node.project) ?: return false
                var suffix = ""

                val isExported = modulite.isExportedFromParent() || modulite.parent() == null
                if (isMultiModulite(node.project, dir)) {
                    if (!isExported) {
                        data.setIcon(PluginIcons.InternalMultiModulite)
                        data.tooltip = "Internal modulite with sub-modulites"
                        suffix = "(internal)"
                    } else {
                        data.setIcon(PluginIcons.MultiModulite)
                        data.tooltip = "Modulite with sub-modulites"
                    }
                } else {
                    if (!isExported) {
                        data.setIcon(PluginIcons.InternalModulite)
                        data.tooltip = "Internal modulite"
                        suffix = "(internal)"
                    } else {
                        data.setIcon(PluginIcons.Modulite)
                        data.tooltip = "Modulite"
                    }
                }

                data.addText(wideSpacer)
                data.addText(
                    ColoredFragment(
                        modulite.foldedName() + " $suffix",
                        SimpleTextAttributes.GRAY_ATTRIBUTES
                    )
                )

                return true
            }

            if (folderIsInternal(node.project, dir)) {
                data.setIcon(PluginIcons.ModuliteInternalFolder)
                data.tooltip = "Modulite internal folder"
            }

            return true
        }

        val allInternal = allFileSymbolsInternal(node.project, node.virtualFile!!)
        if (allInternal) {
            data.addText(wideSpacer)
            data.addText(ColoredFragment("(internal)", SimpleTextAttributes.GRAY_ATTRIBUTES))
        }

        return true
    }

    private fun isMultiModulite(project: Project, folder: VirtualDirectoryImpl, depth: Int = 0): Boolean {
        if (depth > 5) {
            return false
        }

        return folder.children.any {
            if (it is VirtualDirectoryImpl) {
                isMultiModulite(project, it, depth + 1)
            } else {
                it.name == ".modulite.yaml" && depth != 0
            }
        }
    }

    private fun folderIsInternal(project: Project, folder: VirtualDirectoryImpl, depth: Int = 0): Boolean {
        if (depth > 5) {
            return false
        }

        val files = folder.children.filter { it.extension == "php" }
        if (files.isEmpty()) {
            return false
        }

        return folder.children.all {
            if (it is VirtualDirectoryImpl) {
                folderIsInternal(project, it, depth + 1)
            } else {
                allFileSymbolsInternal(project, it)
            }
        }
    }

    private fun allFileSymbolsInternal(project: Project, file: VirtualFile): Boolean {
        val module = file.containingModulite(project) ?: return false
        val psi = PsiManager.getInstance(project).findFile(file) as? PhpFile ?: return false

        if (psi.topLevelDefs.isEmpty) {
            return false
        }

        val allInternal = psi.topLevelDefs.toHashMap().all { (_, value) ->
            value.all {
                !module.isExport(it)
            }
        }

        return allInternal
    }

    private fun hasEmptyColoredTextValue(data: PresentationData) = data.coloredText.isEmpty()

    private fun setName(data: PresentationData) {
        if (hasEmptyColoredTextValue(data)) {
            val presentableText = data.presentableText
            if (presentableText != null) {
                data.addText(presentableText, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}
