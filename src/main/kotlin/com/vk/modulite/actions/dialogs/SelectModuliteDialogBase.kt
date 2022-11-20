package com.vk.modulite.actions.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.bindItemNullable
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.services.ModuliteIndex
import java.awt.Dimension
import javax.swing.JList
import javax.swing.JPanel

abstract class SelectModuliteDialogBase(
    project: Project,
    currentModulite: Modulite?,
    private val labelText: String,
) : DialogWrapper(project, true) {

    data class Model(
        var modulite: Modulite? = null,
    )

    private val modulites = ModuliteIndex.getInstance(project).getModulites().filter { it.name != currentModulite?.name }
    private val model = Model(modulites.firstOrNull())
    private lateinit var mainPanel: DialogPanel

    init {
        title = "Select Modulite"
    }

    fun selected(): Modulite? {
        mainPanel.reset()
        return model.modulite
    }

    open fun renderModulite(renderer: ColoredListCellRenderer<Modulite>, modulite: Modulite) {
        renderer.append(" " + modulite.name)
        renderer.append("  " + modulite.namespace.toString(), SimpleTextAttributes.GRAY_ATTRIBUTES)
    }

    override fun createCenterPanel(): JPanel {
        val renderer = object : ColoredListCellRenderer<Modulite>() {
            override fun customizeCellRenderer(
                list: JList<out Modulite>,
                value: Modulite,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                icon = AllIcons.Nodes.Method
                renderModulite(this, value)
            }
        }

        mainPanel = panel {
            row {
                label(labelText)
            }
            row {
                comboBox(modulites, renderer)
                    .bindItemNullable(model::modulite)
                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }.apply {
            preferredSize = Dimension(400, 0)
        }

        mainPanel.apply()
        return mainPanel
    }
}
