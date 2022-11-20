package com.vk.modulite.actions

import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile

/**
 * Base class for actions that run on a specific YAML [PsiElement].
 *
 * To use, override the [actionPerformed] method and the [errorHint] parameter.
 */
abstract class YamlPsiElementAction<T : PsiElement>(aClass: Class<T>) :
    SelectionBasedPsiElementAction<T>(aClass, YAMLFile::class.java) {

    override fun getElementFromSelection(file: PsiFile, selectionModel: SelectionModel): T? {
        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        return PsiTreeUtil.findElementOfClassAtRange(file, selectionStart, selectionEnd, myClass)
    }
}
