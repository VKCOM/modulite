package com.vk.modulite.highlighting.hints

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.ConstantImpl
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpDefineImpl
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.inspections.intentions.AllowInternalAccessEmptyInspection
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingComposerPackage
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.modulitesWithAllowedAccess
import com.vk.modulite.psi.extensions.php.placesWithAllowedAccess

@Suppress("UnstableApiUsage")
class PhpInlayHintsCollector(
    editor: Editor,
    private val file: PsiFile,
    private val settings: PhpInlayTypeHintsProvider.Settings,
    private val sink: InlayHintsSink
) : BaseInlayHintsCollector(file.project, editor) {

    companion object {
        val LOG = logger<PhpInlayHintsCollector>()
    }

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        // If the indexing process is in progress.
        if (file.project.service<DumbService>().isDumb) return true

        when {
            element is PhpClass && settings.showForClasses -> {
                showAnnotation(element)
            }

            element is MethodImpl &&
                    element.modifier.isPublic &&
                    element.modifier.isStatic &&
                    settings.showForMethods -> {
                showAnnotation(element)
            }

            element is FunctionImpl &&
                    // not closure
                    element.name.isNotEmpty() &&
                    settings.showForFunctions -> {
                // MethodImpl extends FunctionImpl, so if the method is private,
                // we get into this branch, so this should be explicitly prohibited.
                if (element is MethodImpl) {
                    return true
                }
                showAnnotation(element)
            }

            element is ConstantImpl && settings.showForConstants -> {
                showAnnotation(element)
            }

            element is PhpDefineImpl && settings.showForDefines -> {
                showAnnotation(element)
            }
        }

        return true
    }

    private fun showAnnotation(element: PhpNamedElement) {
        val project = element.project

        val moduliteOrPackage = element.containingModulite() ?: element.containingComposerPackage() ?: return
        val configFile = moduliteOrPackage.configFile() ?: return

        // Don't show any hint if composer.json doesn't contain a modulite block.
        if (moduliteOrPackage is ComposerPackage && !moduliteOrPackage.moduliteEnabled) {
            return
        }

        val isExported = moduliteOrPackage.isExport(element)

        val visibility = if (isExported) {
            "exported from"
        } else {
            "internal in"
        }

        val needChangeVisibility = if (element is Method || element is Field) {
            val klass = PsiTreeUtil.findFirstParent(element) { it is PhpClass } as PhpClass?
            val klassIsExported = klass != null && moduliteOrPackage.isExport(klass)

            klassIsExported || isExported
        } else {
            true
        }

        val partiallyVisibleText = if (moduliteOrPackage is Modulite) {
            val modulesWithAccess = element.modulitesWithAllowedAccess(moduliteOrPackage)
            val placesWithAccess = element.placesWithAllowedAccess(moduliteOrPackage)

            val places = mutableListOf<String>()

            if (modulesWithAccess.size > 1) {
                places.add("specific modulites")
            } else if (modulesWithAccess.size == 1) {
                places.add("#m${modulesWithAccess.first()}#m")
            }

            if (placesWithAccess.size > 1) {
                places.add("specific places")
            } else if (placesWithAccess.size == 1) {
                places.add(placesWithAccess.first().toString())
            }

            if (places.isNotEmpty()) {
                "(visible for ${places.joinToString(" and ")})"
            } else {
                ""
            }
        } else {
            ""
        }

        val visibilityPresentation = factory.text(visibility)
        val moduliteNamePresentation = moduliteNamePresentation(moduliteOrPackage.name)
        val partiallyVisibilityPresentation = partiallyVisiblePresentation(partiallyVisibleText)

        val combinedTextPresentation = factory.join(
            listOf(visibilityPresentation, moduliteNamePresentation, partiallyVisibilityPresentation)
        ) { spacePresentation }

        val withActionsPresentationBuilder = MenuActionsBuilder()
            .withAction(
                InlayGoToModuleAction(project, configFile)
            )

        if (moduliteOrPackage is Modulite) {
            withActionsPresentationBuilder
                .withAction(
                    ChangeSymbolVisibilityAction(
                        project, configFile,
                        element, isExported
                    ),
                    needChangeVisibility,
                )
                .withAction(
                    AllowInternalSymbolForModuleAction(
                        moduliteOrPackage, configFile,
                        element, isExported
                    )
                )
                .withAction(
                    ShowHintsSettings()
                )
        }

        val withActionsPresentation = withActionsPresentationBuilder.build(project, combinedTextPresentation)
        val containerPresentation = myFactory.container(withActionsPresentation)

        val openBracketElement = findPlaceForHint(element) ?: return
        val placeAtTheEndOfLine = element !is Constant || element is PhpDefine

        sink.addInlineElement(
            openBracketElement.startOffset,
            relatesToPrecedingText = true,
            presentation = containerPresentation,
            placeAtTheEndOfLine = placeAtTheEndOfLine
        )
    }

    private fun partiallyVisiblePresentation(partiallyVisibleText: String): InlayPresentation {
        val parts = partiallyVisibleText.split("#m")
        val presentationPart = parts.map {
            if (it.startsWith("@")) {
                moduliteNamePresentation(it)
            } else {
                factory.text(it)
            }
        }

        return factory.join(presentationPart) { emptyPresentation }
    }

    private fun findPlaceForHint(element: PhpNamedElement): PsiElement? {
        if (element is ConstantImpl) {
            if (element.parent is AssignmentExpression) {
                return (element.parent as AssignmentExpression).value?.nextSibling
            }
            return element.nextSibling
        }

        if (element is PhpDefineImpl) {
            return element.nextSibling
        }

        return element
    }

    class InlayGoToModuleAction(
        private val project: Project,
        private val moduleFile: VirtualFile,
    ) : AnAction("Go to ${if (moduleFile.name == "composer.json") "Package" else "Modulite"} Definition") {

        override fun actionPerformed(e: AnActionEvent) {
            FileEditorManager.getInstance(project)
                .openTextEditor(OpenFileDescriptor(project, moduleFile), true)
        }
    }

    class AllowInternalSymbolForModuleAction(
        private val module: Modulite,
        private val moduleFile: VirtualFile,
        private val element: PhpNamedElement,
        private val isPublic: Boolean,
    ) : AnAction("Allow Internal Symbol for Specific Modulite") {

        override fun actionPerformed(e: AnActionEvent) {
            invokeLater {
                AllowInternalAccessEmptyInspection.allowInternalAccess(e.project!!, element, module, moduleFile)
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabledAndVisible = e.project != null && !isPublic
        }
    }
}
