package com.vk.modulite.actions.usages.yaml

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.util.SlowOperations
import com.vk.modulite.actions.YamlPsiElementAction
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText

class FindAllowedSymbolUsagesInModuleAction : YamlPsiElementAction<YAMLQuotedText>(YAMLQuotedText::class.java) {
    private var searchModulite: Modulite? = null

    override fun actionPerformed(element: YAMLQuotedText) {
        YamlUsagesFinder(searchModulite).find(element)
    }

    override fun update(e: AnActionEvent, element: YAMLQuotedText?) {
        val modulite = SlowOperations.allowSlowOperations<Modulite?, RuntimeException> {
            e.getData(CommonDataKeys.VIRTUAL_FILE)?.containingModulite(e.project!!)
        }

        if (element == null || modulite == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val insideAllowInternalAccess = YamlUtils.insideAllowInternalAccess(element)
        if (!insideAllowInternalAccess) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val allowedModuliteKeyValue = YamlUtils.getParentKeyValue(element) {
            it.key?.text?.unquote()?.startsWith("@") == true
        }

        if (allowedModuliteKeyValue == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val allowedModuliteName = allowedModuliteKeyValue.key?.text?.unquote() ?: return

        searchModulite = ModuliteIndex.getInstance(e.project!!).getModulite(allowedModuliteName)

        e.presentation.text = "Find Usages in $allowedModuliteName"
    }

    override val errorHint: String = "Error"
}
