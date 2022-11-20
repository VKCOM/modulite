package com.vk.modulite.actions.panels

import com.intellij.ui.ColoredTreeCellRenderer

class RootTreeNode : TreeBaseNode() {
    override fun render(renderer: ColoredTreeCellRenderer) {
        renderer.append("Symbols")
    }
}
