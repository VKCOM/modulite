package com.vk.modulite.highlighting

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.vk.modulite.psi.extensions.yaml.containingModulite
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText

class ModuliteYamlAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is YAMLQuotedText) {
//            highlightSlashes(element, holder)

            if (YamlUtils.insideNamespace(element) || YamlUtils.insideRequires(element)) {
                return
            }

            val modulite = element.containingModulite() ?: return
            val text = element.text.unquote()
            val namespace = "\\\\" + modulite.namespace.toYaml()

            if (text.startsWith(namespace)) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(element.startOffset + 1, element.startOffset + namespace.length + 1))
                    .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                    .create()
            }
        }
    }

//    TODO
//    private fun highlightSlashes(element: YAMLQuotedText, holder: AnnotationHolder) {
//        val text = element.text.unquote()
//
//        val indices = text.indices.filter { text[it] == '\\' }
//
//        for (index in indices) {
//            holder.textAttributes(
//                TextRange(element.startOffset + index + 1, element.startOffset + index + 2),
//                DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
//            )
//        }
//    }

    private fun AnnotationHolder.textAttributes(range: TextRange, textAttributes: TextAttributesKey) {
        newSilentAnnotation(HighlightSeverity.INFORMATION).range(range).textAttributes(textAttributes).create()
    }
}
