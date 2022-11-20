package com.vk.modulite.actions.panels

import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.utils.DocumentationUtils

open class SymbolTreeNode(
    val name: SymbolName,
    val namespace: Namespace,
    private val needEnable: Boolean = true,
    private val suffix: String = "",
) : TreeBaseNode(), TooltipNode {

    override fun render(renderer: ColoredTreeCellRenderer) {
        val attributes = if (needEnable) {
            SimpleTextAttributes.REGULAR_ATTRIBUTES
        } else {
            SimpleTextAttributes.GRAY_ATTRIBUTES
        }

        val icon = when (name.kind) {
            SymbolName.Kind.Modulite -> AllIcons.Nodes.Method
            SymbolName.Kind.ComposerPackage -> AllIcons.Nodes.Package
            SymbolName.Kind.Class -> AllIcons.Nodes.Class
            SymbolName.Kind.Field -> AllIcons.Nodes.Field
            SymbolName.Kind.Method -> AllIcons.Nodes.Method
            SymbolName.Kind.ClassConstant -> AllIcons.Nodes.Constant
            SymbolName.Kind.Function -> AllIcons.Nodes.Function
            SymbolName.Kind.GlobalVariable -> AllIcons.Nodes.Variable
            SymbolName.Kind.Constant -> AllIcons.Nodes.Constant
            else -> null
        }

        val relativeName = name.relative(namespace)

        var showName = relativeName.toString().removePrefix("\\")
        if (name.kind == SymbolName.Kind.ComposerPackage) {
            showName = showName.removePrefix("#")
        }

        renderer.icon = icon
        renderer.append(" $showName", attributes)
        if (suffix.isNotEmpty()) {
            renderer.append(suffix, attributes)
        }
    }

    override fun getTooltip(): String {
        val typeString = when (name.kind) {
            SymbolName.Kind.Modulite -> "Modulite"
            SymbolName.Kind.ComposerPackage -> "Composer package"
            SymbolName.Kind.Class -> "Class"
            SymbolName.Kind.Field -> "Field"
            SymbolName.Kind.Method -> "Method"
            SymbolName.Kind.ClassConstant -> "Class constant"
            SymbolName.Kind.Function -> "Function"
            SymbolName.Kind.GlobalVariable -> "Global variable"
            SymbolName.Kind.Constant -> "Constant"
            else -> "Unknown"
        }

        val normalizedName = name.toString().removePrefix("\\")
        val name = DocumentationUtils.colorize(normalizedName, DocumentationUtils.asDeclaration)

        return "$typeString <code>$name</code>"
    }
}
