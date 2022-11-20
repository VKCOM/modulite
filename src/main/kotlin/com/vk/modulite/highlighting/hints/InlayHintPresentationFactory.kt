package com.vk.modulite.highlighting.hints

import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl

@Suppress("UnstableApiUsage")
class InlayHintPresentationFactory(private val editor: Editor) {
    private val textMetricsStorage = InlayTextMetricsStorage(editor as EditorImpl)
    private val offsetFromTopProvider = object : InsetValueProvider {
        override val top =
            textMetricsStorage.getFontMetrics(false).offsetFromTop()
    }

    private val baseFactory = PresentationFactory(editor as EditorImpl)

    fun folding(placeholder: InlayPresentation, unwrapAction: () -> InlayPresentation): InlayPresentation {
        return ChangeOnClickPresentation(baseFactory.changeOnHover(placeholder, onHover = {
            withInlayAttributes(placeholder, EditorColors.FOLDED_TEXT_ATTRIBUTES)
        }), onClick = unwrapAction)
    }

    fun container(base: InlayPresentation): InlayPresentation {
        val rounding = InsetPresentation(
            base, left = 7, right = 7,
            top = 0, down = 0
        )

        return DynamicInsetPresentation(rounding, offsetFromTopProvider)
    }

    private fun withInlayAttributes(base: InlayPresentation, attributes: TextAttributesKey) =
        WithAttributesPresentation(
            base, attributes, editor,
            WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        )
}
