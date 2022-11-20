package com.vk.modulite.modulite

import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidatorEx
import com.intellij.util.ProcessingContext
import com.vk.modulite.psi.ModuliteNamePsi

class ModuliteRenameValidator : RenameInputValidatorEx {
    private var needShowWarning = true

    override fun getPattern(): ElementPattern<out PsiElement> = PlatformPatterns.psiElement(ModuliteNamePsi::class.java)

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        if (element !is ModuliteNamePsi) return true
        if (!newName.startsWith("@")) return true
        if (newName.contains(" ")) return true

        needShowWarning = false
        return true
    }

    override fun getErrorMessage(newName: String, project: Project) =
        if (needShowWarning) "Modulite name should start with @ and not contain spaces" else null
}
