package com.vk.modulite.inspections.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.vk.modulite.psi.extensions.files.containingModulite

class GoToModuliteDefinitionIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "Go to modulite definition"
    override fun getText() = "Go to modulite definition"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.containingModulite() != null && element.parent is PhpPsiElement
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val modulite = element.containingModulite() ?: return
        val configFile = modulite.configFile() ?: return

        FileEditorManager.getInstance(project)
            .openTextEditor(OpenFileDescriptor(project, configFile), true)
    }
}
