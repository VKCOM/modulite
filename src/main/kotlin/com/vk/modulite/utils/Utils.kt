package com.vk.modulite.utils

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.uiDesigner.core.GridLayoutManager
import com.vk.modulite.notifications.ModuliteNotification
import java.awt.Insets
import javax.swing.JPanel

object Utils {
    val LOG = logger<Utils>()

    fun <T> writeCommand(project: Project, block: () -> T): T {
        val result = Ref.create<T>()

        WriteCommandAction.runWriteCommandAction(project) {
            result.set(block())
        }

        return result.get()
    }

    fun <T> runTransparent(block: () -> T): T {
        val res = Ref<T>()

        CommandProcessor.getInstance().runUndoTransparentAction {
            res.set(block())
        }

        return res.get()
    }

    inline fun runBackground(
        project: Project,
        @NlsContexts.ProgressTitle title: String,
        crossinline task: (indicator: ProgressIndicator) -> Unit,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, title, true,
        ) {
            override fun run(indicator: ProgressIndicator) {
                LOG.info("Start Background task ('$title')")
                indicator.isIndeterminate = true
                task(indicator)
                LOG.info("End Background task ('$title')")
            }
        })
    }

    inline fun runModal(
        project: Project,
        @NlsContexts.DialogTitle title: String,
        crossinline task: (indicator: ProgressIndicator) -> Unit,
    ) {
        ProgressManager.getInstance().run(object : Task.Modal(
            project, title, true,
        ) {
            override fun run(indicator: ProgressIndicator) {
                LOG.info("Start ConditionalModal task ('$title')")
                indicator.isIndeterminate = true
                task(indicator)
                LOG.info("End ConditionalModal task ('$title')")
            }
        })
    }
}

fun String.unquote(): String {
    if (length >= 2 &&
        (get(0) == '"' && get(length - 1) == '"' || get(0) == '\'' && get(length - 1) == '\'')
    ) {
        return substring(1, length - 1)
    }

    return this
}

fun String.quote(): String {
    return "\"$this\""
}

fun String.normalizedFqn(): String {
    return replace("\\\\", "\\")
}

fun String.normalizedPath(): String {
    return replace("\\", "/")
}

fun String.toKebabCase(): String {
    // If "RPC" for example, then we want "rpc", not "r-p-c"
    val onlyUpperCaseCharacters = all { it.isUpperCase() }
    if (onlyUpperCaseCharacters) {
        return lowercase()
    }

    val sb = StringBuilder()
    for (i in indices) {
        val c = get(i)
        if (c.isUpperCase()) {
            if (i > 0) {
                sb.append('-')
            }
            sb.append(c.lowercaseChar())
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}

fun VirtualFile.fromStubs(): Boolean {
    return path.contains("/stubs/") || path.contains("/Stubs/") || path.contains("/phpstorm-stubs/")
}

fun VirtualFile.fromVendor(): Boolean {
    return path.contains("/vendor/")
}

fun VirtualFile.fromTests(): Boolean {
    return path.contains("/tests/")
}

inline fun <reified T : PsiElement> PsiElement.childOfType(): T? {
    var found: T? = null
    PsiTreeUtil.processElements(this) {
        if (it is T) {
            found = it
            return@processElements false
        }
        true
    }

    return found
}

fun gotItNotification(project: Project, key: String, title: String, message: String) {
    val dontNeedShow = PropertiesComponent.getInstance(project).getBoolean(key)
    if (dontNeedShow) {
        return
    }

    ModuliteNotification(message)
        .withTitle(title)
        .withActions(
            ModuliteNotification.Action("Don't show again") { _, notification ->
                notification.expire()
                PropertiesComponent.getInstance(project).setValue(key, true)
            }
        )
        .show()
}

fun spacer(height: Int): JPanel {
    return JPanel().apply {
        layout = GridLayoutManager(
            1, 1,
            Insets(0, 0, height, 0), -1, -1
        )
    }
}

fun InspectionManager.createModuliteProblemDescriptor(
    psiElement: PsiElement,
    rangeInElement: TextRange?,
    descriptionTemplate: @InspectionMessage String,
    highlightType: ProblemHighlightType,
    onTheFly: Boolean,
    vararg fixes: LocalQuickFix?,
) = createProblemDescriptor(
    psiElement,
    rangeInElement,
    "[modulite] $descriptionTemplate",
    highlightType,
    onTheFly,
    *fixes
)

fun ProblemsHolder.registerModuliteProblem(
    psiElement: PsiElement,
    descriptionTemplate: @InspectionMessage String,
    highlightType: ProblemHighlightType,
    vararg fixes: LocalQuickFix?,
) {
    registerProblem(
        psiElement,
        "[modulite] $descriptionTemplate",
        highlightType,
        *fixes
    )
}
