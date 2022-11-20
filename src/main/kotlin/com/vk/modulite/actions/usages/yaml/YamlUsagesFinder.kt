package com.vk.modulite.actions.usages.yaml

import com.intellij.psi.PsiElement
import com.vk.modulite.actions.usages.base.UsagesBaseFinder
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.resolveSymbol
import org.jetbrains.yaml.psi.YAMLQuotedText

class YamlUsagesFinder(private val modulite: Modulite? = null) : UsagesBaseFinder() {
    override fun resolveSearchElement(element: PsiElement): PsiElement? {
        if (element !is YAMLQuotedText) return null

        val refs = element.resolveSymbol()
        if (refs.isEmpty()) {
            return null
        }
        return refs.firstOrNull()
    }

    override fun resolveSearchModulite(element: PsiElement): Modulite? = modulite ?: element.containingModulite()
}
