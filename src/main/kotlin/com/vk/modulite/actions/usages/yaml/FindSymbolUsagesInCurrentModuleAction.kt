package com.vk.modulite.actions.usages.yaml

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.util.SlowOperations
import com.vk.modulite.actions.YamlPsiElementAction
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.utils.YamlUtils
import org.jetbrains.yaml.psi.YAMLQuotedText

class FindSymbolUsagesInCurrentModuleAction : YamlPsiElementAction<YAMLQuotedText>(YAMLQuotedText::class.java) {
    override fun actionPerformed(element: YAMLQuotedText) {
        YamlUsagesFinder().find(element)
    }

    override fun update(e: AnActionEvent, element: YAMLQuotedText?) {
        val modulite =
            SlowOperations.allowSlowOperations<Modulite?, RuntimeException> {
                e.getData(CommonDataKeys.VIRTUAL_FILE)?.containingModulite(e.project!!)
            }

        if (element == null || modulite == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val insideRequires = YamlUtils.insideRequires(element)
        if (!insideRequires) {
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

        e.presentation.text = "Find Usages in ${modulite.name}"
    }

    override val errorHint: String = "Error"
}
