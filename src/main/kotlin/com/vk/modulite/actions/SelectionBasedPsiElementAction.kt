package com.vk.modulite.actions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.util.containers.ContainerUtil

/**
 * Base class for actions that run on a specific [PsiElement].
 */
abstract class SelectionBasedPsiElementAction<T : PsiElement>(
    protected val myClass: Class<T>,
    protected val myFileClass: Class<out PsiFile>
) : AnAction() {

    protected abstract fun actionPerformed(element: T)
    protected abstract fun update(e: AnActionEvent, element: T?)
    protected abstract val errorHint: String

    final override fun actionPerformed(e: AnActionEvent) {
        val editor = getEditor(e)
        val file = getPsiFile(e)
        if (editor == null || file == null) {
            return
        }

        val expressions = getElement(editor, file)
        val first = ContainerUtil.getFirstItem(expressions)

        if (expressions.size > 1) {
            IntroduceTargetChooser.showChooser(editor, expressions, object : Pass<T>() {
                override fun pass(expression: T) {
                    actionPerformed(expression)
                }
            }) { expression: T -> expression.text }
        } else if (expressions.size == 1 && first != null) {
            actionPerformed(first)
        } else {
            showError(editor)
        }
    }

    final override fun update(e: AnActionEvent) {
        val editor = getEditor(e)
        val file = getPsiFile(e)
        if (editor == null || file == null) {
            return
        }

        val expressions = getElement(editor, file)
        val first = ContainerUtil.getFirstItem(expressions)

        if (expressions.size > 1) {
            IntroduceTargetChooser.showChooser(editor, expressions, object : Pass<T>() {
                override fun pass(expression: T) {
                    update(e, expression)
                }
            }) { expression: T -> expression.text }
        } else if (expressions.size == 1 && first != null) {
            update(e, first)
        } else {
            update(e, null)
        }
    }

    private fun showError(editor: Editor) {
        ApplicationManager.getApplication().invokeLater {
            val errorHint =
                "Cannot find element of class " + myClass.simpleName + " at selection/offset"
            HintManager.getInstance().showErrorHint(editor, errorHint)
        }
    }

    private fun getElement(editor: Editor, file: PsiFile): List<T> {
        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            val sel = getElementFromSelection(file, selectionModel) ?: return emptyList()
            listOf(sel)
        } else {
            getElementAtOffset(editor, file)
        }
    }

    private fun getElementAtOffset(editor: Editor, file: PsiFile): List<T> {
        val el = PsiTreeUtil.findElementOfClassAtOffset(
            file, editor.caretModel.offset,
            myClass, false
        ) ?: return emptyList()

        return listOf(el)
    }

    protected open fun getElementFromSelection(file: PsiFile, selectionModel: SelectionModel): T? {
        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        return PsiTreeUtil.findElementOfClassAtRange(file, selectionStart, selectionEnd, myClass)
    }

    companion object {
        private fun getEditor(e: AnActionEvent) = e.getData(CommonDataKeys.EDITOR)
        private fun getPsiFile(e: AnActionEvent) = e.getData(CommonDataKeys.PSI_FILE)
    }
}
