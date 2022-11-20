package com.vk.modulite.actions.usages.php

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfTypes
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.vk.modulite.actions.usages.base.UsagesBaseFinder
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite

class PhpUsagesFinder(private val modulite: Modulite? = null) : UsagesBaseFinder() {
    override fun resolveSearchElement(element: PsiElement): PsiElement? {
        val elements =
            when (val el = element.parentOfTypes(PhpReference::class, PhpNamedElement::class, withSelf = true)) {
                is FunctionReference -> {
                    if (el.name == "define") {
                        listOf(el.parent)
                    } else {
                        el.resolveGlobal(false)
                    }
                }
                is PhpReference -> el.resolveGlobal(false)
                is PhpNamedElement -> listOf(el)
                else -> return null
            }

        return elements.firstOrNull()
    }

    override fun resolveSearchModulite(element: PsiElement): Modulite? = modulite ?: element.containingModulite()
}
