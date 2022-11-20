package com.vk.modulite.actions.usages.yaml

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.modulite.actions.YamlPsiElementAction
import com.vk.modulite.actions.dialogs.SelectFindUsagesModuliteDialog
import com.vk.modulite.psi.extensions.files.containingModulite
import org.jetbrains.yaml.psi.YAMLQuotedText

class FindSymbolUsagesInSelectedModuleAction : YamlPsiElementAction<YAMLQuotedText>(YAMLQuotedText::class.java) {
    override fun actionPerformed(element: YAMLQuotedText) {
        val modulite = SelectFindUsagesModuliteDialog.request(element.project, element.containingModulite()) ?: return
        YamlUsagesFinder(modulite).find(element)
    }

    override fun update(e: AnActionEvent, element: YAMLQuotedText?) {
        if (element == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val value = element.text
        val isModuliteName = value.contains("@")
        val isComposerPackageName = value.contains("#")
        if (isModuliteName || isComposerPackageName) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Find Usages in Modulite..."
    }

    override val errorHint: String = "Error"
}
