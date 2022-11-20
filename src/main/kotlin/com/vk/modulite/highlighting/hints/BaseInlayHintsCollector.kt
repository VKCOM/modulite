package com.vk.modulite.highlighting.hints

import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingModel
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation
import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.psi.extensions.files.psiFile
import com.vk.modulite.psi.extensions.yaml.makeElementExport
import com.vk.modulite.psi.extensions.yaml.makeElementInternal
import com.vk.modulite.psi.extensions.yaml.makeModuliteExport
import com.vk.modulite.psi.extensions.yaml.makeModuliteInternal
import com.vk.modulite.services.ModuliteIndex
import org.jetbrains.yaml.psi.YAMLFile

@Suppress("UnstableApiUsage")
abstract class BaseInlayHintsCollector(val project: Project, editor: Editor) : FactoryInlayHintsCollector(editor) {
    val myFactory = InlayHintPresentationFactory(editor)
    val spacePresentation = factory.text(" ")
    val emptyPresentation = factory.text("")

    fun moduliteNamePresentation(moduliteName: String): InlayPresentation {
        val parts = moduliteName.split("/")
        if (parts.size == 1) {
            return singleModuliteNamePresentation(moduliteName, moduliteName)
        }

        val indices = moduliteName.indices.filter { moduliteName[it] == '/' }

        val presentations = mutableListOf<InlayPresentation>()
        var prevIndex = 0
        for (i in indices) {
            val name = moduliteName.substring(0, i)
            val nameToShow = moduliteName.substring(prevIndex, i)
            presentations.add(singleModuliteNamePresentation(name, nameToShow))
            prevIndex = i + 1
        }

        if (moduliteName.length > 20) {
            // @.../name
            // expand to:
            // @parent/parent2/name
            presentations.add(factory.text(""))
            val joinedParent = factory.join(presentations) { factory.text("/") }
            val foldedParent = myFactory.folding(factory.text("@.../")) { joinedParent }

            val name = moduliteName.substring(0, moduliteName.length)
            val nameToShow = moduliteName.substring(prevIndex, moduliteName.length)
            val namePresentation = singleModuliteNamePresentation(name, nameToShow)

            return factory.join(listOf(foldedParent, namePresentation)) { emptyPresentation }
        }

        val name = moduliteName.substring(0, moduliteName.length)
        val nameToShow = moduliteName.substring(prevIndex, moduliteName.length)
        presentations.add(singleModuliteNamePresentation(name, nameToShow))

        return factory.join(presentations) { factory.text("/") }
    }

    private fun singleModuliteNamePresentation(name: String, nameToShow: String): InlayPresentation {
        val textPresentation = factory.text(nameToShow)
        val onClickPresentation = factory.referenceOnHover(textPresentation) { _, _ ->
            val file = ModuliteIndex.getInstance(project).getModulite(name)?.configFile()
                ?: return@referenceOnHover

            FileEditorManager.getInstance(project)
                .openTextEditor(OpenFileDescriptor(project, file), true)
        }

        return onClickPresentation
    }

    class ChangeSymbolVisibilityAction(
        private val project: Project,
        private val moduliteConfig: VirtualFile,
        private val element: PhpNamedElement,
        private val isPublic: Boolean,
    ) : AnAction() {

        override fun actionPerformed(e: AnActionEvent) {
            val psiFile = moduliteConfig.psiFile<YAMLFile>(project) ?: return

            if (!isPublic) {
                psiFile.makeElementExport(element)
            } else {
                psiFile.makeElementInternal(element)
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.text = if (isPublic) "Make Internal" else "Make Exported"
        }
    }

    class ChangeModuliteVisibilityAction(
        private val project: Project,
        private val moduliteConfig: VirtualFile,
        private val element: ModuliteNamePsi,
        private val isPublic: Boolean,
    ) : AnAction() {

        override fun actionPerformed(e: AnActionEvent) {
            val psiFile = moduliteConfig.psiFile<YAMLFile>(project) ?: return

            if (!isPublic) {
                psiFile.makeModuliteExport(element)
            } else {
                psiFile.makeModuliteInternal(element)
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.text = if (isPublic) "Make Internal" else "Make Exported"
        }
    }

    class ShowHintsSettings : AnAction("Hints Settings...") {
        override fun getActionUpdateThread() = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            showHintsSettingsDialog(e)
        }

        private fun showHintsSettingsDialog(e: AnActionEvent) {
            val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
            val fileLanguage = file.language

            InlayHintsConfigurable.showSettingsDialogForLanguage(file.project, fileLanguage) {
                it is CodeVisionGroupSettingModel
            }
        }
    }

    class MenuActionsBuilder {
        private val actions = mutableListOf<AnAction>()

        fun withAction(action: AnAction, needAdd: Boolean = true): MenuActionsBuilder {
            if (needAdd) {
                actions.add(action)
            }
            return this
        }

        fun build(project: Project, inlay: InlayPresentation): InlayPresentation =
            InsetPresentation(
                MenuOnClickPresentation(inlay, project) { actions }, left = 1
            )
    }
}
