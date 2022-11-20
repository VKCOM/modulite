package com.vk.modulite.utils

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.jetbrains.php.lang.highlighter.PhpHighlightingData
import io.ktor.util.*

object DocumentationUtils {
    private fun loadKey(key: TextAttributesKey): TextAttributes =
        EditorColorsManager.getInstance().globalScheme.getAttributes(key)!!

    val asKeyword = loadKey(PhpHighlightingData.KEYWORD)
    val asIdentifier = loadKey(PhpHighlightingData.IDENTIFIER)
    val asDeclaration = loadKey(DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)

    @Suppress("UnstableApiUsage")
    fun StringBuilder.colorize(text: String, attrs: TextAttributes) {
        HtmlSyntaxInfoUtil.appendStyledSpan(
            this, attrs, text.escapeHTML(),
            DocumentationSettings.getHighlightingSaturation(false)
        )
    }

    fun colorize(text: String, attrs: TextAttributes): String {
        val sb = StringBuilder()
        sb.colorize(text, attrs)
        return sb.toString()
    }
}
