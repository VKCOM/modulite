package com.vk.modulite.highlighting

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.php.lang.psi.elements.*
import com.vk.modulite.modulite.ModuliteRestrictionChecker
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.utils.fromStubs
import com.vk.modulite.utils.fromTests
import com.vk.modulite.utils.fromVendor

class ModulitePhpAnnotator : Annotator {
    companion object {
        private val LOG = logger<ModulitePhpAnnotator>()
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is MethodReference -> {
                val methodNamePsi = element.firstChild?.nextSibling?.nextSibling
                holder.annotateReferenceUsage(element, methodNamePsi)
            }
            is FunctionReference -> {
                val problemElement = if (element.firstChild is PhpNamespaceReference) element
                else element.firstChild
                holder.annotateReferenceUsage(element, problemElement)
            }
            is FieldReference, is ConstantReference, is ClassConstantReference, is ClassReference -> {
                holder.annotateReferenceUsage(element as PhpReference)
            }
            is Global -> {
                element.variables.forEach {
                    holder.annotateReferenceUsage(it)
                }
            }
        }
    }

    private fun AnnotationHolder.annotateReferenceUsage(
        reference: PhpReference,
        problemElement: PsiElement? = reference
    ) {
        val references = reference.resolveGlobal(false)
        if (references.isEmpty()) {
//            LOG.warn("Unknown reference for symbol '${reference.safeFqn()}'")
            return
        }

        val filteredReferences = references.filter {
            val file = it.containingFile.virtualFile
            !file.fromTests() && !file.fromVendor() && !file.fromStubs() && it !is PhpNamespace
        }

        val problemPsiElement = problemElement ?: reference
        val element = filteredReferences.firstOrNull() ?: return
        val elementModulite = element.containingModulite() ?: return

        val context = ModuliteRestrictionChecker.createContext(reference)
        val isAllowedForInternal = elementModulite.symbolInAllowedInternalAccess(element, context, reference)

        if (isAllowedForInternal) {
            // TODO: tests
            newAnnotation(
                HighlightSeverity.INFORMATION,
                "Symbol ${element.fqn} is internal in ${elementModulite.name}. You can access it here as an exception, however it's not recommended to add new usages as this symbol is being refactored.",
            )
                .range(TextRange(problemPsiElement.startOffset, problemPsiElement.endOffset))
                .highlightType(ProblemHighlightType.LIKE_DEPRECATED)
                .create()
        }
    }
}
