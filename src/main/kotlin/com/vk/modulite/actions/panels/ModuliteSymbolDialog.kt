package com.vk.modulite.actions.panels

import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.services.ModuliteDeps
import com.vk.modulite.utils.spacer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.tree.TreeSelectionModel

class ModuliteSymbolDialog(
    project: Project,
    selectedModulite: Modulite,
    private val currentModulite: Modulite,
    symbols: List<SymbolName>,
    private val dependencies: ModuliteDeps,
) : DialogWrapper(project) {

    companion object {
        fun requestSymbol(
            project: Project,
            selectedModulite: Modulite,
            currentModulite: Modulite,
            symbols: List<SymbolName>,
            dependencies: ModuliteDeps,
        ): SymbolName? {
            val dialog = ModuliteSymbolDialog(project, selectedModulite, currentModulite, symbols, dependencies)
            if (!dialog.showAndGet()) {
                return null
            }
            return dialog.getSelectedSymbol()
        }
    }

    private val symbolsTree = object : SymbolsTreeBase(symbols) {
        init {
            init()
        }

        override fun onDoubleClick(node: CheckedTreeNode) {
            if (node is SymbolTreeNode) {
                node.isChecked = true
                close(OK_EXIT_CODE)
            }
        }

        override fun selectionModel() = TreeSelectionModel.SINGLE_TREE_SELECTION

        override fun tooltipAdditionalInfo(node: TooltipNode): String? {
            return if (node is CheckedTreeNode) {
                "Double-click on a symbol to show usages"
            } else {
                null
            }
        }

        override fun needCheckboxes(): Boolean = false

        override fun createNode(symbol: SymbolName): SymbolTreeNode {
            val isUsed = dependencies.symbols.contains(symbol)
            val postfix = if (!isUsed) " (not used in ${currentModulite.name})" else ""
            val node = SymbolTreeNode(symbol, symbol.namespace(), needEnable = isUsed, postfix)
            // Если символ не используется внутри модуля, то делаем его узел неактивным
            node.isEnabled = isUsed
            node.isChecked = false
            return node
        }
    }

    init {
        title = "Modulite ${selectedModulite.name} Symbols"

        init()
    }

    fun getSelectedSymbol() = symbolsTree.getSelectedSymbols().firstOrNull()

    override fun createCenterPanel(): JComponent {
        return JBUI.Panels.simplePanel()
            .addToTop(
                JBUI.Panels.simplePanel()
                    .addToLeft(JLabel("Double-click on a symbol to show its usages in $currentModulite:"))
                    .addToBottom(spacer(5))
            )
            .addToCenter(
                symbolsTree.component()
                    .apply {
                        border = IdeBorderFactory.createBorder()
                    }
            )
            .apply {
                preferredSize = JBDimension(600, 250)
            }
    }
}
