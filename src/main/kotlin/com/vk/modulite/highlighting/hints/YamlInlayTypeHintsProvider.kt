package com.vk.modulite.highlighting.hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class YamlInlayTypeHintsProvider : InlayHintsProvider<YamlInlayTypeHintsProvider.Settings> {
    data class Settings(
        var showForModulite: Boolean = true,
        var showForSequence: Boolean = true,
    )

    override val group = InlayGroup.CODE_VISION_GROUP_NEW
    override val description = "Show modulite visibility hint in config"
    override val key = KEY
    override val name = "Modulite YAML visibility hints"
    override val previewText = null

    override fun createConfigurable(settings: Settings) = object : ImmediateConfigurable {
        override val mainCheckboxText: String = "Use inline hints for visibility"

        override val cases = listOf(
            Case("Show for modulites", "modulite.hints.modulites", settings::showForModulite),
            Case("Show for sequences", "modulite.hints.sequences", settings::showForSequence),
        )

        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun getCaseDescription(case: Case): String? = when (case.id) {
        "modulite.hints.modulites" -> "Show visibility hint next to the modulite name."
        "modulite.hints.sequences" -> "Show visibility hint in export/require sequences."
        else -> null
    }

    override fun createSettings() = Settings()

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: Settings, sink: InlayHintsSink) =
        YamlInlayHintsCollector(editor, file, settings, sink)

    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("modulite.hints")
    }
}
