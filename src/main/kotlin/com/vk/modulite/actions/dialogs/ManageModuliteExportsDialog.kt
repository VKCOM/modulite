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
import javax.swing.JComponent
import javax.swing.border.Border
import javax.swing.tree.TreeSelectionModel

class ManageModuliteExportsDialog(
    project: Project,
    private val modulite: Modulite,
) : DialogWrapper(project) {

    private lateinit var mainPanel: DialogPanel

    private val symbols = modulite.symbols()
    private val symbolsTree = DependenciesTree(symbols)

    init {
        title = "Manage ${modulite.name} Exports"

        setOKButtonText("Save")

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

            row("Exported Symbols:") {}
            row {
                cell(symbolsTree.component())
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
            }
                .topGap(TopGap.NONE)
        }

        mainPanel.reset()

        return mainPanel.apply {
            preferredSize = JBDimension(450, height)
        }
    }

    class DependenciesTree(symbols: List<SymbolName>) : SymbolsTreeBase(symbols) {
        init {
            init()
        }

        override fun needCheckboxes() = true

        override fun onDoubleClick(node: CheckedTreeNode) {}

        override fun borders(): Border = IdeBorderFactory.createBorder()

        override fun selectionModel() = TreeSelectionModel.SINGLE_TREE_SELECTION

        override fun tooltipAdditionalInfo(node: TooltipNode) = null

        override fun createNode(symbol: SymbolName) = SymbolTreeNode(symbol, symbol.namespace())

        override fun dimension() = JBDimension(300, 400)
    }
}
