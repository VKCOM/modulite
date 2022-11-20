package com.vk.modulite.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.php.modulitesWithAllowedAccess

class SelectAllowInternalModuliteDialog(
    project: Project,
    element: PhpNamedElement,
    currentModulite: Modulite?,
) : SelectModuliteDialogBase(
    project,
    currentModulite,
    "Select a modulite to allow the use of an internal symbol:",
) {
    companion object {
        fun request(
            project: Project,
            element: PhpNamedElement,
            currentModulite: Modulite?,
        ): Modulite? {
            val dialog = SelectAllowInternalModuliteDialog(project, element, currentModulite)
            if (!dialog.showAndGet()) {
                return null
            }
            return dialog.selected()
        }
    }

    private val modulitesWithAllowedAccess =
        if (currentModulite == null) emptyList() else element.modulitesWithAllowedAccess(currentModulite)

    init {
        init()
    }

    override fun renderModulite(renderer: ColoredListCellRenderer<Modulite>, modulite: Modulite) {
        super.renderModulite(renderer, modulite)
        val alreadyAllowed = if (modulitesWithAllowedAccess.contains(modulite.symbolName())) "(already allowed)" else ""
        renderer.append(" $alreadyAllowed", SimpleTextAttributes.GRAY_ATTRIBUTES)
    }
}
