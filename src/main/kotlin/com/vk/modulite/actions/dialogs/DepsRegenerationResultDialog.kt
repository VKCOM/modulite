package com.vk.modulite.actions.dialogs

import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBDimension
import com.vk.modulite.SymbolName
import com.vk.modulite.actions.panels.SymbolTreeNode
import com.vk.modulite.actions.panels.SymbolsTreeBase
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.services.ModuliteDepsDiff
import javax.swing.JComponent
import javax.swing.border.Border
import javax.swing.tree.TreeSelectionModel

class DepsRegenerationResultDialog(
    project: Project,
    private val modulite: Modulite,
    diff: ModuliteDepsDiff,
) : DialogWrapper(project) {

    private lateinit var mainPanel: DialogPanel

    private val addedSymbols = diff.added()
    private val removedSymbols = diff.removed()

    private val addedSymbolsTree = DependenciesTree(addedSymbols)
    private val removedSymbolsTree = DependenciesTree(removedSymbols)

    init {
        title = "Dependencies Regeneration Result"
        init()
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = panel {
            row("Modulite:") {
                icon(AllIcons.Nodes.Method)
                    .gap(RightGap.SMALL)
                label(modulite.name)
                    .gap(RightGap.SMALL)
                comment(modulite.namespace.toString())
            }

            row("Added:") {}
                .visible(addedSymbols.isNotEmpty())
            row {
                cell(addedSymbolsTree.component())
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
            }.visible(addedSymbols.isNotEmpty())
                .topGap(TopGap.NONE)

            row("Removed:") {}
                .visible(removedSymbols.isNotEmpty())
            row {
                cell(removedSymbolsTree.component())
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
            }.visible(removedSymbols.isNotEmpty())
                .topGap(TopGap.NONE)
        }

        mainPanel.reset()

        return mainPanel.apply {
            preferredSize = JBDimension(450, height)
        }
    }

    override fun createActions() = arrayOf(myOKAction)

    class DependenciesTree(symbols: List<SymbolName>) : SymbolsTreeBase(symbols) {
        init {
            init()
        }

        override fun needCheckboxes() = false

        override fun onDoubleClick(node: CheckedTreeNode) {}

        override fun borders(): Border = IdeBorderFactory.createBorder()

        override fun selectionModel() = TreeSelectionModel.SINGLE_TREE_SELECTION

        override fun tooltipAdditionalInfo(node: TooltipNode) = null

        override fun createNode(symbol: SymbolName) = SymbolTreeNode(symbol, symbol.namespace())

        override fun dimension() = JBDimension(300, 250)
    }
}
