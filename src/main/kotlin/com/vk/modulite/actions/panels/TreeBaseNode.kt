package com.vk.modulite.actions.panels

import com.intellij.dvcs.push.OutgoingResult
import com.intellij.dvcs.push.ui.EditableTreeNode
import com.intellij.ui.CheckedTreeNode
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JTree

abstract class TreeBaseNode : CheckedTreeNode(null), EditableTreeNode {
    override fun fireOnChange() {}
    override fun fireOnCancel() {}
    override fun fireOnSelectionChange(isSelected: Boolean) {}
    override fun cancelLoading() {}
    override fun startLoading(tree: JTree, future: Future<AtomicReference<OutgoingResult>>, initial: Boolean) {}
    override fun isEditableNow(): Boolean {
        return false
    }
}
