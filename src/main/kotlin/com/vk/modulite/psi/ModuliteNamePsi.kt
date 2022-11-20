package com.vk.modulite.psi

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.util.IncorrectOperationException
import com.vk.modulite.SymbolName
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.YamlUtils.getTopLevelKey
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlPsiElementVisitor
import org.jetbrains.yaml.psi.impl.YAMLQuotedTextImpl
import org.jetbrains.yaml.psi.impl.YAMLScalarImpl
import org.jetbrains.yaml.psi.impl.YamlScalarTextEvaluator

open class ModuliteNamePsi(node: ASTNode) : YAMLScalarImpl(node), YAMLQuotedText, PsiNamedElement, PsiReference {
    private val textPsi = YAMLQuotedTextImpl(node)

    fun modulite() = ModuliteIndex.getInstance(project).getModulite(name)

    fun symbolName() = SymbolName(name, fromYaml = true)

    override fun isMultiline() = textPsi.isMultiline

    override fun isSingleQuote() = textPsi.isSingleQuote

    override fun getContentRanges(): MutableList<TextRange> = textPsi.contentRanges

    override fun getTextEvaluator(): YamlScalarTextEvaluator<*> = textPsi.textEvaluator

    override fun getName(): String = text.unquote()

    override fun setName(newElementName: String): PsiElement {
        val modulite = modulite()

        if (name != newElementName) {
            val children = modulite?.children() ?: emptyList()

            children.forEach {
                val psi = it.namePsi() ?: return@forEach

                val currentChildName = it.name.substringAfterLast('/')
                val newChildName = "$newElementName/$currentChildName"

                // TODO: может привести к взаимной блокировки при взаимной редактирование файла
                // A (module name) -> A.B (module name) -> A.B (export) in A
                RenameProcessor(project, psi, newChildName, false, false).doRun()
            }

            val newName = newElementName.removePrefix("@")
            val newQuotedString = YamlUtils.createQuotedText(project, "@$newName")
            if (newQuotedString.node == null) return this

            val newNode = newQuotedString.node
            return replace(ModuliteNamePsi(newNode))
        }

        return this
    }

    private fun moduliteExists(name: String) = ModuliteIndex.getInstance(element.project).getModulite(name) != null

    override fun getReferences(): Array<PsiReference> {
        val text = text
        if (!text.contains('/')) {
            // if just @name
            val name = text.unquote()
            val ref = if (moduliteExists(name)) this else UnknownModuleReference(element, name = name)
            return arrayOf(ref)
        }

        // if @name/other/path
        val indices = text.indices.filter { text[it] == '/' }

        val references = mutableListOf<PsiReference>()
        var startIndex = 1
        for (i in indices) {
            val range = TextRange(startIndex, i)
            val nameRef = object : ModuliteNamePsi(node) {
                override fun getRangeInElement() = range
                override fun getElement(): PsiElement = this@ModuliteNamePsi
            }

            val name = text.substring(1, i)
            val ref = if (moduliteExists(name)) nameRef else UnknownModuleReference(element, range, name)
            references.add(ref)
            startIndex = i + 1
        }

        val range = TextRange(startIndex, text.length - 1)
        val nameRef = object : ModuliteNamePsi(node) {
            override fun getRangeInElement() = range
            override fun getElement(): PsiElement = this@ModuliteNamePsi
        }

        val name = text.substring(1, text.length - 1)
        val ref = if (moduliteExists(name)) nameRef else UnknownModuleReference(element, range, name)
        references.add(ref)

        return references.toTypedArray()
    }

    class UnknownModuleReference(
        element: PsiElement,
        private val range: TextRange = TextRange(1, element.textLength - 1),
        val name: String,
    ) : PsiReferenceBase<PsiElement?>(element) {

        private val currentModulite = element.containingModulite()

        override fun resolve(): PsiElement? = null

        override fun getRangeInElement() = range

        override fun getVariants(): Array<Any> {
            val index = ModuliteIndex.getInstance(element.project)
            val variants = mutableListOf<LookupElement>()

            index.getModulites().forEach {
                if (it.name == currentModulite?.name)
                    return@forEach

                if (!name.contains('/')) {
                    variants.add(LookupElementBuilder.create(it.name))
                    return@forEach
                }

                val nameBeforeLastSlash = name.substringBeforeLast('/')
                if (!it.name.startsWith("$nameBeforeLastSlash/")) {
                    return@forEach
                }
                val variantText = it.name.removePrefix("$nameBeforeLastSlash/")
                variants.add(LookupElementBuilder.create(variantText))
            }

            return variants.toTypedArray()
        }
    }

    override fun getElement(): PsiElement = this

    override fun getRangeInElement(): TextRange = TextRange(1, node.textLength - 1)

    override fun resolve(): PsiElement? {
        val name = if (rangeInElement.endOffset != node.textLength - 1) {
            // if parent module
            // @parent/child
            // ^^^^^^^
            name.substring(0, rangeInElement.endOffset - 1)
        } else {
            // if child module
            // @parent/child
            //         ^^^^^
            name
        }

        val file = ModuliteIndex.getInstance(element.project).getConfigFile(name) ?: return null

        val psiFile = PsiManager.getInstance(element.project).findFile(file) ?: return null

        val yamlFile = psiFile as? YAMLFile ?: return null
        val nameKey = yamlFile.getTopLevelKey("name") ?: return null

        return nameKey.value
    }

    override fun getCanonicalText(): String = name

    override fun handleElementRename(newElementName: String): PsiElement = setName(newElementName)

    override fun bindToElement(element: PsiElement) = throw IncorrectOperationException("Not supported")

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is ModuliteNamePsi) {
            return element.name == name
        }
        return false
    }

    override fun isSoft(): Boolean = false

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is YamlPsiElementVisitor) {
            visitor.visitQuotedText(this)
        } else {
            super.accept(visitor)
        }
    }
}
