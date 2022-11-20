package com.vk.modulite.actions.panels

import com.intellij.dvcs.push.ui.EditableTreeNode
import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.ui.DefaultTreeUI
import com.intellij.util.containers.Convertor
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.SymbolName.Kind
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.JViewport
import javax.swing.border.Border
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

abstract class SymbolsTreeBase(private val symbols: List<SymbolName>) {
    companion object {
        private val LOG = logger<SymbolsTreeBase>()
    }

    private val northPanel = JPanel(BorderLayout())
    private val scrollPane: JScrollPane
    private var tree = CheckboxTree()
    private val treeCellRenderer = TreeCellRenderer()
    private val treeRoot = RootTreeNode()
    private val treeExpander: TreeExpander

    init {
        tree = object : CheckboxTree(treeCellRenderer, treeRoot) {
            override fun shouldShowBusyIconIfNeeded(): Boolean = true

            override fun isPathEditable(path: TreePath): Boolean = false

            override fun onDoubleClick(node: CheckedTreeNode) {
                onDoubleClickProxy(node)
            }

            override fun getToolTipText(event: MouseEvent): String {
                val path = tree.getPathForLocation(event.x, event.y) ?: return ""
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return ""
                if (node is TooltipNode) {
                    val additionalInfo = tooltipAdditionalInfo(node) ?: return node.tooltip
                    return node.tooltip + "<p style='font-style:italic;color:gray;'>" + additionalInfo + "</p>"
                }
                return ""
            }

            override fun installSpeedSearch() {
                TreeSpeedSearch(this, Convertor { path: TreePath ->
                    val pathComponent = path.lastPathComponent
                    if (pathComponent is SymbolTreeNode) {
                        return@Convertor pathComponent.name.toString()
                    }

                    pathComponent.toString()
                })
            }
        }

        val group = DefaultActionGroup()
        val actionManager = CommonActionsManager.getInstance()

        treeExpander = DefaultTreeExpander(tree)
        group.add(actionManager.createExpandAllAction(treeExpander, tree))
        group.add(actionManager.createCollapseAllAction(treeExpander, tree))

        val treeToolbar = ActionManager.getInstance().createActionToolbar("SymbolsTree", group, true)
        treeToolbar.targetComponent = tree

        scrollPane = JBScrollPane(tree)

        northPanel.add(treeToolbar.component, BorderLayout.NORTH)
        northPanel.add(scrollPane, BorderLayout.CENTER)
    }

    fun init() {
        buildTree(symbols)

        tree.setUI(DefaultTreeUI())
        tree.border = JBUI.Borders.emptyTop(10)
        tree.isEditable = false
        tree.invokesStopCellEditing = true
        tree.isRootVisible = false
        tree.rowHeight = 0
        tree.showsRootHandles = true

        tree.selectionModel?.selectionMode = selectionModel()
        tree.selectionPath = TreeUtil.getFirstNodePath(tree)

        treeExpander.expandAll()

        scrollPane.viewport.scrollMode = JViewport.SIMPLE_SCROLL_MODE
        scrollPane.isOpaque = false
        scrollPane.border = IdeBorderFactory.createBorder(SideBorder.TOP)

        scrollPane.preferredSize = dimension()

        northPanel.border = borders()
        northPanel.preferredSize = dimension()
    }

    fun component() = northPanel

    fun tree() = tree

    abstract fun onDoubleClick(node: CheckedTreeNode)
    abstract fun selectionModel(): Int
    abstract fun tooltipAdditionalInfo(node: TooltipNode): String?

    open fun borders(): Border = IdeBorderFactory.createBorder()

    open fun needCheckboxes(): Boolean = true

    open fun createNode(symbol: SymbolName) = SymbolTreeNode(symbol, Namespace())

    open fun dimension() = if (symbols.isEmpty()) {
        JBDimension(0, 40)
    } else {
        JBDimension(0, 150)
    }

    open fun modulitesGroupName(): String = "Modulites"

    open fun composerPackagesGroupName(): String = "Composer Packages"

    open fun globalsGroupName(): String = "Global Symbols"

    fun getCheckedSymbols(): List<SymbolName> {
        val checkedNodes = getCheckedTreeNodes()
        return if (checkedNodes.isEmpty()) emptyList()
        else checkedNodes.mapNotNull {
            if (it is SymbolTreeNode) it.name else null
        }
    }

    fun getSelectedSymbols(): List<SymbolName> {
        val selectedSymbols = getSelectedTreeNodes()
        return if (selectedSymbols.isEmpty()) emptyList()
        else selectedSymbols.mapNotNull {
            if (it is SymbolTreeNode) it.name else null
        }
    }

    private fun onDoubleClickProxy(node: CheckedTreeNode) = onDoubleClick(node)

    private fun groupByFirst(root: TreeBaseNode, rawNames: List<SymbolName>, names: List<List<String>>) {
        val groups = names.groupBy { it[0] }
        groups.forEach { (key, value) ->
            val children = value.map { it.drop(1) }
            val allEmpty = (children.filter { it.isNotEmpty() }).isEmpty()
            if (allEmpty || children.any { it.isEmpty() }) {
                var fqn = if (root is SymbolGroupTreeNode) root.name else ""
                var current: TreeBaseNode? = root
                while (current !is RootTreeNode) {
                    current = current?.parent as? SymbolGroupTreeNode
                    if (current == null) {
                        break
                    }
                    fqn = current.name + fqn
                }
                fqn = if (fqn.isNotEmpty()) "\\" + fqn + key else "\\" + key

                val name = rawNames.find { it.name == fqn }
                if (name != null) {
                    root.add(createNode(name))
                } else {
                    LOG.warn("Could not find symbol $fqn")
                }

                // Если не все пусты, то нужно добавить все подгруппы
                if (allEmpty) {
                    return@forEach
                }
            }
            val groupNode = SymbolGroupTreeNode(key + "\\")
            root.add(groupNode)
            groupByFirst(groupNode, rawNames, children.filter { it.isNotEmpty() })
        }
    }

    private fun buildTree(symbols: List<SymbolName>) {
        val modulites = symbols.filter { it.kind == Kind.Modulite }
        buildModulitesTree(modulites)

        val composerPackages = symbols.filter { it.kind == Kind.ComposerPackage }
        buildComposerPackagesTree(composerPackages)

        val globals = symbols.filter { it.isGlobal() }
        buildGlobalsTree(globals)

        val treeNames = symbols
            .filter { !it.isGlobal() }
            .sortedBy { it.name }
            .sortedBy { it.kind }
            .map { it.name.removePrefix("\\").split("\\") }

        groupByFirst(treeRoot, symbols, treeNames)
    }

    private fun buildModulitesTree(modulites: List<SymbolName>) {
        if (modulites.isEmpty()) return

        val groupNode = SymbolGroupTreeNode(modulitesGroupName())
        modulites
            .sortedBy { it.name }
            .forEach {
                groupNode.add(createNode(it))
            }

        treeRoot.add(groupNode)
    }

    private fun buildComposerPackagesTree(composerPackages: List<SymbolName>) {
        if (composerPackages.isEmpty()) return

        val groupNode = SymbolGroupTreeNode(composerPackagesGroupName())
        composerPackages
            .sortedBy { it.name }
            .forEach {
                groupNode.add(createNode(it))
            }

        treeRoot.add(groupNode)
    }

    private fun buildGlobalsTree(globals: List<SymbolName>) {
        if (globals.isEmpty()) return

        val groupNode = SymbolGroupTreeNode(globalsGroupName())
        globals
            .sortedBy { it.name }
            .sortedBy { it.kind }
            .forEach {
                groupNode.add(createNode(it))
            }

        treeRoot.add(groupNode)
    }

    private fun getCheckedTreeNodes(): List<DefaultMutableTreeNode> {
        val nodes = getNodesForRows((0 until tree.rowCount).toList())
        return nodes.filter {
            it is CheckedTreeNode && it.isChecked
        }
    }

    private fun getSelectedTreeNodes(): List<DefaultMutableTreeNode> {
        val rows = tree.selectionRows
        return if (rows != null && rows.isNotEmpty())
            getNodesForRows(getSortedRows(rows))
        else
            emptyList()
    }

    private fun getSortedRows(rows: IntArray): List<Int> {
        val sorted = mutableListOf<Int>()
        rows.forEach {
            sorted.add(it)
        }
        sorted.sortWith(Collections.reverseOrder())
        return sorted
    }

    private fun getNodesForRows(rows: List<Int>): List<DefaultMutableTreeNode> {
        val nodes = mutableListOf<DefaultMutableTreeNode>()
        rows.forEach {
            val path = tree.getPathForRow(it)
            val pathComponent = path?.lastPathComponent
            if (pathComponent is DefaultMutableTreeNode) {
                nodes.add(pathComponent)
            }
        }
        return nodes
    }

    private inner class TreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ) {
            if (value !is DefaultMutableTreeNode) return

            val withCheckboxes = needCheckboxes()
            myCheckbox.border = null
            myCheckbox.isVisible = withCheckboxes

            val renderer = textRenderer

            if (withCheckboxes) {
                renderer.ipad = JBUI.insets(0, 10)
            }

            val userObject = value.userObject

            if (value is EditableTreeNode) {
                value.render(renderer)
            } else {
                renderer.append(userObject?.toString() ?: "")
            }
        }
    }
}
