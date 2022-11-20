package com.vk.modulite.actions.dialogs

import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.applyIf
import com.intellij.util.ui.JBDimension
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.actions.panels.SymbolTreeNode
import com.vk.modulite.actions.panels.SymbolsTreeBase
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.services.ModuliteIndex
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.TreeSelectionModel

class ModuliteBuilderData(
    val name: String,
    val namespace: Namespace,
    val description: String?,
    val folder: String,
    val exported: Boolean,
    val selectedSymbols: List<SymbolName>,
    val parent: Modulite?
)

class ModuliteWizardDialog(
    project: Project,
    parent: Modulite?,
    folder: VirtualFile,
    name: String = "",
    symbols: List<SymbolName> = listOf(),
    namespace: Namespace = Namespace("\\"),
    fromSource: Boolean,
) : DialogWrapper(project) {

    data class Model(
        var folder: String,
        var name: String,
        var description: String,
        var namespace: Namespace,
        var exportFromParent: Boolean,
        var fromSource: Boolean,
        var dir: VirtualFile,
        var parent: Modulite?,
        var symbols: List<SymbolName>,
    )

    private val initialNamespace = if (parent != null && namespace.isGlobal()) parent.namespace else namespace
    private val model = Model(
        folder = if (fromSource) folder.name else "",
        name = if (parent != null) parent.name + "/" + name else name,
        description = "",
        namespace = initialNamespace.deepClonePolymorphic(),
        exportFromParent = false,
        fromSource = fromSource,
        dir = folder,
        parent = parent,
        symbols = symbols,
    )

    private lateinit var mainPanel: DialogPanel

    private val modulites = ModuliteIndex.getInstance(project).getModulites()

    private val symbolsTree = object : SymbolsTreeBase(symbols) {
        init {
            init()
        }

        override fun onDoubleClick(node: CheckedTreeNode) {
            node.isChecked = !node.isChecked
            tree().repaint()
        }

        override fun selectionModel() = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION

        override fun modulitesGroupName() = "Nested modulites"

        override fun composerPackagesGroupName() = "Nested composer packages"

        override fun tooltipAdditionalInfo(node: TooltipNode): String? {
            return if (node is CheckedTreeNode && node.isChecked) {
                "Deselect a symbol to make it internal"
            } else if (node is CheckedTreeNode) {
                "Select a symbol to make it exported"
            } else {
                null
            }
        }

        override fun createNode(symbol: SymbolName): SymbolTreeNode {
            return SymbolTreeNode(symbol, symbol.namespace())
        }
    }

    init {
        title = "New Modulite"
        init()
    }

    fun data() = ModuliteBuilderData(
        name = name(),
        namespace = namespace(),
        description = description(),
        folder = folder(),
        exported = exported(),
        selectedSymbols = selectedSymbols(),
        parent = parent(),
    )

    fun selectedSymbols(): List<SymbolName> = symbolsTree.getCheckedSymbols()
    fun name(): String = "@" + model.name.removePrefix("@")
    fun namespace(): Namespace = model.namespace
    fun description(): String? = model.description.ifEmpty { null }
    fun exported(): Boolean = model.exportFromParent
    fun folder(): String = model.folder
    fun parent(): Modulite? = model.parent

    override fun createCenterPanel(): JComponent {
        mainPanel = panel {
            row("Name:") {
                textField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(model::name)
                    .errorOnApply("Name should have at least one character") {
                        it.text.isEmpty()
                    }
                    .errorOnApply("Incorrect name, enter name after /") {
                        it.text.endsWith("/")
                    }
                    .errorOnApply("Modulite already exists") {
                        modulites.any { mod -> mod.name == it.text }
                    }
            }

            row("Parent:") {
                icon(AllIcons.Nodes.Method)
                    .gap(RightGap.SMALL)
                if (model.parent != null) {
                    label(model.parent!!.name)
                        .gap(RightGap.SMALL)
                    comment(model.parent!!.namespace.toPHP())
                }
            }.visible(model.parent != null)

            row("Description:") {
                expandableTextField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(model::description)
            }

            row("Folder:") {
                textField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(model::folder)
                    .enabled(!model.fromSource)
                    .comment("A folder to place the new modulite.")
                    .errorOnApply("Folder should not be empty") {
                        it.text.isEmpty()
                    }
                    .errorOnApply("Folder already exists") {
                        model.dir.findChild(it.text) != null
                    }
                    .errorOnApply("Invalid folder name") {
                        model.folder.contains(" ") || model.folder.contains("/")
                    }
                    .onChange { folderAfter ->
                        val folderBefore = model.folder
                        mainPanel.apply()

                        val justConcat = model.parent != null && !folderBefore.endsWith("\\")

                        if (justConcat) {
                            model.namespace = Namespace("$initialNamespace$folderAfter")
                        } else {
                            val last = model.namespace.last()
                            if (last == folderBefore || folderAfter.length == 1) {
                                model.namespace = model.namespace.replaceLast(folderAfter.trim())
                            }
                        }

                        mainPanel.reset()
                    }
            }

            row("Namespace:") {
                expandableTextField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText({
                        val ns = model.namespace.toString()
                        ns.applyIf(ns != "\\") {
                            removeSuffix("\\")
                        }
                    }, {
                        model.namespace = Namespace(it)
                    })
                    .comment("Base namespace for the modulite.")
                    .enabled(!model.fromSource || model.symbols.isEmpty())
                    .errorOnApply("Namespace should not be empty (use \\ for global namespace)") {
                        it.text.isEmpty()
                    }
                    .errorOnApply("Incorrect namespace, enter a name after \\") {
                        it.text.endsWith("\\") && it.text != "\\"
                    }
            }.bottomGap(BottomGap.NONE)

            row("Visibility:") {
                if (model.parent != null) {
                    checkBox("Export from")
                        .bindSelected(model::exportFromParent)
                        .comment("Modulite will be visible outside of the parent.")
                        .gap(RightGap.SMALL)
                    icon(AllIcons.Nodes.Method)
                        .gap(RightGap.SMALL)
                    label(model.parent!!.name)
                        .gap(RightGap.SMALL)
                }
            }.visible(model.parent != null)

            group("Exported Symbols", indent = false) {
                row {
                    cell(symbolsTree.component())
                        .horizontalAlign(HorizontalAlign.FILL)
                        .verticalAlign(VerticalAlign.FILL)
                        .comment("Selected symbols will be public, the rest will be internal and will not be available outside of the modulite.", 90)
                }
            }
                .visible(model.fromSource)
                .topGap(TopGap.NONE)
        }

        mainPanel.reset()

        return mainPanel.apply {
            val height = if (model.fromSource) 380 else 180
            preferredSize = JBDimension(450, height)
        }
    }

    private fun Cell<JBTextField>.onChange(cb: (String) -> Unit): Cell<JBTextField> {
        component.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                cb(component.text)
            }

            override fun removeUpdate(e: DocumentEvent) {
                cb(component.text)
            }

            override fun changedUpdate(e: DocumentEvent) {
                cb(component.text)
            }
        })

        return this
    }
}
