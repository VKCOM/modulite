package com.vk.modulite.actions.panels

import com.intellij.ui.ColoredTreeCellRenderer

class SymbolGroupTreeNode(val name: String) : TreeBaseNode() {
    override fun render(renderer: ColoredTreeCellRenderer) {
        renderer.append(name)
    }
}
