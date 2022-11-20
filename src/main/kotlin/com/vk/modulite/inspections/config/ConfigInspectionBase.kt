package com.vk.modulite.inspections.config

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiElement

abstract class ConfigInspectionBase : LocalInspectionTool() {
    override fun isSuppressedFor(element: PsiElement) = if (element.containingFile.virtualFile.name != ".modulite.yaml") {
        true
    } else {
        super.isSuppressedFor(element)
    }
}
