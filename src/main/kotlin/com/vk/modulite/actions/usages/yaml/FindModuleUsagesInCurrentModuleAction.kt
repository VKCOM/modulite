package com.vk.modulite.actions.usages.yaml

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.util.SlowOperations
import com.vk.modulite.actions.YamlPsiElementAction
import com.vk.modulite.actions.panels.ModuliteSymbolDialog
import com.vk.modulite.actions.usages.base.UsagesBaseFinder
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText

class FindModuleUsagesInCurrentModuleAction : YamlPsiElementAction<YAMLQuotedText>(YAMLQuotedText::class.java) {
    private val base = object : UsagesBaseFinder() {
        override fun resolveSearchElement(element: PsiElement): PsiElement? {
            val project = element.project
            val selectedModuleName = element.text.unquote()
            val selectedModule = ModuliteIndex.getInstance(project).getModulite(selectedModuleName) ?: return null
            val currentModule = element.containingModulite() ?: return null

            val symbols = selectedModule.symbols()
            val dependencies = currentModule.dependencies(collapseModuleSymbols = false)
            val selectedSymbol =
                ModuliteSymbolDialog.requestSymbol(project, selectedModule, currentModule, symbols, dependencies)
                    ?: throw AbortActionException()

            val refs = selectedSymbol.resolve(project)
            return refs.firstOrNull()
        }

        override fun resolveSearchModulite(element: PsiElement): Modulite? = element.containingModulite()
    }

    override fun actionPerformed(element: YAMLQuotedText) {
        base.find(element)
    }

    override fun update(e: AnActionEvent, element: YAMLQuotedText?) {
        val module =
            SlowOperations.allowSlowOperations<Modulite?, RuntimeException> {
                e.getData(CommonDataKeys.VIRTUAL_FILE)?.containingModulite(e.project!!)
            }

        if (element == null || module == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val insideRequires = YamlUtils.insideRequires(element)
        if (!insideRequires) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val isModuleName = element.text.contains("@")
        if (!isModuleName) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Find Usages in ${module.name}"
    }

    override val errorHint: String = "Error"
}
