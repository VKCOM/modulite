package com.vk.modulite.highlighting.hints

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteDependenciesManager
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.extractSymbolsList
import com.vk.modulite.psi.extensions.yaml.resolveSymbolName
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

@Suppress("UnstableApiUsage")
class YamlInlayHintsCollector(
    editor: Editor,
    private val file: PsiFile,
    private val settings: YamlInlayTypeHintsProvider.Settings,
    private val sink: InlayHintsSink,
) : BaseInlayHintsCollector(file.project, editor) {

    private val spaceWidth = EditorUtil.getPlainSpaceWidth(editor)
    private var currentModulite: Modulite? = null
    private var currentSeq: YAMLSequence? = null
    private var currentSymbolsList = emptyList<SymbolName>()
    private var currentStartsElements = mutableListOf<SymbolName>()
    private var currentCountByKind = mutableMapOf<SymbolName.Kind, Int>()

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        // If the indexing process is in progress.
        if (file.project.service<DumbService>().isDumb) return false

        when {
            element is ModuliteNamePsi && settings.showForModulite -> {
                showAnnotation(element)
            }

            element is YAMLSequence &&
                    (YamlUtils.insideRequires(element) || YamlUtils.insideExport(element)) &&
                    settings.showForSequence                       -> {
                setCurrentSeq(element)
            }

            element is YAMLSequenceItem &&
                    (YamlUtils.insideRequires(element) || YamlUtils.insideExport(element)) &&
                    settings.showForSequence                       -> {
                showGroupAnnotation(element)
            }

            element is YAMLKeyValue &&
                    settings.showForSequence                       -> {
                showExportAnnotation(element)
                showRequireAnnotation(element)
            }
        }

        return true
    }

    private fun showAnnotation(element: ModuliteNamePsi) {
        if (!YamlUtils.insideName(element)) {
            return
        }

        val project = element.project
        val modulite = element.containingModulite() ?: return
        val parent = modulite.parent() ?: return
        val parentConfig = parent.configFile() ?: return
        val isExported = modulite.isExportedFromParent()

        val visibility = if (isExported) {
            "exported from"
        } else {
            "internal in"
        }

        val visibilityPresentation = factory.text(visibility)
        val parentNamePresentation = moduliteNamePresentation(parent.name)

        val combinedTextPresentation = factory.join(
            listOf(visibilityPresentation, parentNamePresentation)
        ) { spacePresentation }

        val withActionsPresentationBuilder = MenuActionsBuilder()
            .withAction(
                InlayGoToParentAction(project, parentConfig),
            )
            .withAction(
                ChangeModuliteVisibilityAction(
                    project, parentConfig,
                    element, isExported
                ),
            )
            .withAction(
                ShowHintsSettings()
            )

        val withActionsPresentation = withActionsPresentationBuilder.build(project, combinedTextPresentation)
        val containerPresentation = myFactory.container(withActionsPresentation)

        sink.addInlineElement(
            offset = element.endOffset,
            relatesToPrecedingText = false,
            presentation = containerPresentation,
            placeAtTheEndOfLine = false
        )
    }

    class InlayGoToParentAction(
        private val project: Project,
        private val moduliteConfig: VirtualFile,
    ) : AnAction("Go to Parent Modulite") {

        override fun actionPerformed(e: AnActionEvent) {
            FileEditorManager.getInstance(project)
                .openTextEditor(OpenFileDescriptor(project, moduliteConfig), true)
        }
    }

    private fun setCurrentSeq(element: YAMLSequence) {
        currentSeq = element
        currentSymbolsList = element.extractSymbolsList()
        currentStartsElements = mutableListOf()
        currentCountByKind = mutableMapOf()
        currentModulite = element.containingModulite()

        if (currentSymbolsList.isNotEmpty() && currentModulite != null) {
            val first = currentSymbolsList.first().absolutize(currentModulite!!)
            var currentType = first.kind
            currentStartsElements.add(first)

            currentSymbolsList.forEach {
                currentCountByKind[it.kind] = currentCountByKind.getOrDefault(it.kind, 0) + 1

                if (it.kind != currentType) {
                    currentType = it.kind
                    currentStartsElements.add(it.absolutize(currentModulite!!))
                }
            }
        }
    }

    private fun showGroupAnnotation(element: YAMLSequenceItem) {
        val symbolName = (element.value as? YAMLQuotedText)?.resolveSymbolName()

        if (currentStartsElements.contains(symbolName) && symbolName != null) {
            val count = currentCountByKind[symbolName.kind] ?: 0
            val kindName = symbolName.kindReadableName(many = count > 1)
            val simpleInsetPresentation = factory.inset(
                factory.smallText("$count $kindName"),
                spaceWidth * 2, 2, 9, 2
            )

            val withActionsPresentationBuilder = MenuActionsBuilder()
                .withAction(
                    ShowHintsSettings()
                )

            val withActionsPresentation = withActionsPresentationBuilder.build(project, simpleInsetPresentation)

            sink.addBlockElement(
                offset = element.endOffset,
                showAbove = true,
                priority = 1,
                relatesToPrecedingText = true,
                presentation = withActionsPresentation,
            )
        }
    }

    private fun showExportAnnotation(element: YAMLKeyValue) {
        if (element.key?.text?.unquote() != "export") {
            return
        }

        val modulite = element.containingModulite() ?: return
        val countExports = modulite.exportList.size

        addTextHintWithAction("$countExports total", element.key!!) {
            // TODO: ManageModuliteExportsDialog(project, modulite).showAndGet()
        }
    }

    private fun showRequireAnnotation(element: YAMLKeyValue) {
        if (element.key?.text?.unquote() != "require") {
            return
        }

        val modulite = element.containingModulite() ?: return
        val configFile = modulite.configFile() ?: return
        val countRequires = modulite.requires.symbols.size

        addTextHintWithAction("$countRequires total, click to regenerate", element.key!!) {
            ModuliteDependenciesManager.regenerate(project, configFile)
        }
    }

    private fun addTextHintWithAction(text: String, element: PsiElement, action: () -> Unit) {
        val simpleInsetPresentation = factory.inset(
            factory.smallText(text), 0, 2, 7, 2
        )

        val onClickPresentation = factory.referenceOnHover(simpleInsetPresentation) { _, _ ->
            action()
        }

        val withActionsPresentationBuilder = MenuActionsBuilder()
            .withAction(
                ShowHintsSettings()
            )

        val withActionsPresentation = withActionsPresentationBuilder.build(project, onClickPresentation)

        sink.addBlockElement(
            offset = element.endOffset,
            showAbove = true,
            priority = 1,
            relatesToPrecedingText = true,
            presentation = withActionsPresentation,
        )
    }

    companion object {
        val LOG = logger<YamlInlayHintsCollector>()
    }
}
