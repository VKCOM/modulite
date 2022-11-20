package com.vk.modulite.highlighting.hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class PhpInlayTypeHintsProvider : InlayHintsProvider<PhpInlayTypeHintsProvider.Settings> {
    data class Settings(
        var showForClasses: Boolean = true,
        var showForFunctions: Boolean = true,
        var showForMethods: Boolean = true,
        var showForVariables: Boolean = true,
        var showForConstants: Boolean = true,
        var showForDefines: Boolean = true,
    )

    override val group = InlayGroup.CODE_VISION_GROUP_NEW
    override val description = "Show modulite visibility hints for PHP symbols"
    override val key = KEY
    override val name = "Modulite PHP visibility hints"
    override val previewText = null

    override fun createConfigurable(settings: Settings) = object : ImmediateConfigurable {
        override val mainCheckboxText = "Use inline hints for visibility"

        override val cases = listOf(
            Case("Show for classes", "modulite.hints.classes", settings::showForClasses),
            Case("Show for functions", "modulite.hints.functions", settings::showForFunctions),
            Case("Show for methods", "modulite.hints.methods", settings::showForMethods),
            Case("Show for global variables", "modulite.hints.globals", settings::showForVariables),
            Case("Show for constants", "modulite.hints.constants", settings::showForConstants),
            Case("Show for defines", "modulite.hints.defines", settings::showForDefines),
        )

        override fun createComponent(listener: ChangeListener) = JPanel()
    }

    override fun getCaseDescription(case: Case): String? = when (case.id) {
        "modulite.hints.classes" -> "Show visibility hint next to the class name."
        "modulite.hints.functions" -> "Show visibility hint next to the function name."
        "modulite.hints.methods" -> "Show visibility hint next to the method name."
        "modulite.hints.globals" -> "Show visibility hint next to the global variable name."
        "modulite.hints.constants" -> "Show visibility hint next to the constant name."
        "modulite.hints.defines" -> "Show visibility hint next to the constant name created by define()."
        else -> null
    }

    override fun createSettings() = Settings()

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: Settings, sink: InlayHintsSink) =
        PhpInlayHintsCollector(editor, file, settings, sink)

    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("modulite.hints")
    }
}
