package com.vk.modulite.highlighting

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.psi.extensions.json.composerPackage
import com.vk.modulite.utils.DocumentationUtils.asDeclaration
import com.vk.modulite.utils.DocumentationUtils.asIdentifier
import com.vk.modulite.utils.DocumentationUtils.asKeyword
import com.vk.modulite.utils.DocumentationUtils.colorize

class ModuliteDocumentProvider : DocumentationProvider {
    // when hovering with cmd — very short, one line info
    override fun getQuickNavigateInfo(element: PsiElement, hoverElement: PsiElement) = when (element) {
        is ModuliteNamePsi   -> generateModuliteShortInfo(element)
        is JsonStringLiteral -> generateComposerPackageShortInfo(element)
        else                 -> null
    }

    private fun generateModuliteShortInfo(element: ModuliteNamePsi): String? {
        val modulite = element.modulite() ?: return null

        return StringBuilder().apply {
            append("<pre>")
            colorize("modulite", asKeyword)
            append(" ")
            colorize(modulite.name, asDeclaration)
            append(" ")
            colorize(modulite.namespace.toPHP(), asIdentifier)
            append("</pre>")
        }.toString()
    }

    private fun generateComposerPackageShortInfo(element: JsonStringLiteral): String? {
        val composerPackage = element.composerPackage() ?: return null
        val name = composerPackage.name.removePrefix("#")

        return StringBuilder().apply {
            append("<pre>")
            colorize("composer package", asKeyword)
            append(" ")
            colorize(name, asDeclaration)
            append("</pre>")
        }.toString()
    }

    // when just hovering — a bit more detailed (but still only most important) info
    override fun generateDoc(element: PsiElement, hoverElement: PsiElement?) = when (element) {
        is ModuliteNamePsi   -> generateModuliteDoc(element)
        is JsonStringLiteral -> generateComposerPackageDoc(element)
        else                 -> null
    }

    private fun generateModuliteDoc(element: ModuliteNamePsi): String? {
        val modulite = element.modulite() ?: return null

        val buffer = StringBuilder().apply {
            append("<div class='definition'><pre>")

            colorize("modulite", asKeyword)
            append(" ")
            colorize(modulite.name, asDeclaration)
            append(" ")
            colorize(modulite.namespace.toPHP(), asIdentifier)

            append("</pre></div>")

            append("<table class='sections'>")

            colorize(modulite.description, asIdentifier)

            append("</table>")
        }

        return buffer.toString().ifEmpty { null }
    }

    private fun generateComposerPackageDoc(element: JsonStringLiteral): String? {
        val composerPackage = element.composerPackage() ?: return null
        val name = composerPackage.name.removePrefix("#")

        val buffer = StringBuilder().apply {
            append("<div class='definition'><pre>")

            colorize("composer package", asKeyword)
            append(" ")
            colorize(name, asDeclaration)

            append("</pre></div>")

            append("<table class='sections'>")
            colorize(composerPackage.description, asIdentifier)
            append("</table>")
        }

        return buffer.toString().ifEmpty { null }
    }
}
