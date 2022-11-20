package com.vk.modulite.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.PhpDocLinkResolver
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpNamespace
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.relatedModulite
import com.vk.modulite.services.ComposerPackagesIndex
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.normalizedFqn
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLPsiElement

class ModuliteYamlReferenceContributor : PsiReferenceContributor() {
    companion object {
        private val LOG = logger<ModuliteYamlReferenceContributor>()

        fun YAMLPsiElement.references(): Array<PsiReference> {
            return PhpPsiReferenceProvider()
                .getReferencesByElement(this, ProcessingContext())
        }
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLElementTypes.SCALAR_QUOTED_STRING),
            PhpPsiReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY),
            PhpPsiReferenceProvider()
        )
    }

    class PhpPsiReferenceProvider : PsiReferenceProvider() {
        private var globalPrefix = Namespace()

        private fun resolveName(name: String, element: PsiElement): SymbolName {
            if (YamlUtils.insideRequires(element)) {
                return SymbolName(name)
            }

            val fixName = name.removeSuffix("IntellijIdeaRulezzz ")

            if (fixName.startsWith("\\")) {
                return SymbolName(fixName)
            }

            if (globalPrefix.isGlobal()) {
                return SymbolName("\\" + fixName)
            }

            return SymbolName("$globalPrefix$fixName")
        }

        override fun getReferencesByElement(
            element: PsiElement,
            context: ProcessingContext
        ): Array<PsiReference> {
            val needRef = YamlUtils.insideName(element) ||
                    YamlUtils.insideExport(element) ||
                    YamlUtils.insideForceInternal(element) ||
                    YamlUtils.insideRequires(element) ||
                    YamlUtils.insideAllowInternalAccess(element)

            if (!needRef) {
                return emptyArray()
            }

            calcGlobalPrefix(element)

            val rawText = element.text
            val unquotedText = rawText.unquote()

            if (unquotedText.startsWith("#")) {
                return handleComposerPackageName(unquotedText, element)
            }

            val fqn = unquotedText.normalizedFqn()

            if (element is ModuliteNamePsi) {
                return element.references
            }

            if (fqn.contains("::$")) {
                return handleMemberName(fqn, rawText, element)
            }

            if (fqn.contains("::")) {
                return handleMemberName(fqn, rawText, element)
            }

            if (fqn.contains("$")) {
                return handleGlobalVariable(fqn, element)
            }

            return handleClassName(fqn, element)
        }

        private fun calcGlobalPrefix(element: PsiElement) {
            val inAllowBlock = YamlUtils.insideAllowInternalAccess(element)
            val inExportBlock = YamlUtils.insideExport(element)
            val inInternalBlock = YamlUtils.insideForceInternal(element)
            if (!inAllowBlock && !inExportBlock && !inInternalBlock) {
                return
            }

            // TODO: разобраться
            val file = element.containingFile as YAMLFile
            val virtualFile = file.virtualFile

            val modulite = if (virtualFile == null) {
                LOG.warn("Containing file is null")
                file.relatedModulite()
            } else {
                virtualFile.containingModulite(element.project)
            } ?: return

            globalPrefix = modulite.namespace
        }

        private fun handleGlobalVariable(text: String, element: PsiElement): Array<PsiReference> {
            val result = PhpDocLinkResolver.resolve(resolveName(text, element).toString(), element)
            return result.map {
                it.element.getNamespaceAndClassReferences(element)
            }.flatten().toTypedArray()
        }

        private fun PsiElement.getNamespaceAndClassReferences(
            textElement: PsiElement,
            endIndex: Int = textElement.textLength - 1
        ): List<PsiReference> {
            val text = textElement.text.unquote()
            val countSlashes = text.count { it == '\\' }

            // if no namespace
            if (countSlashes == 0) {
                return listOf(
                    PhpElementReference(
                        textElement, this,
                        TextRange(1, endIndex)
                    )
                )
            }

            if (countSlashes % 2 != 0) {
                return emptyList()
            }

            val lastSlashIndex = text.lastIndexOf("\\\\")

            val references = mutableListOf<PsiReference>(
                PhpElementReference(textElement, this, TextRange(lastSlashIndex + 3, endIndex))
            )

            if (lastSlashIndex != 0) {
                val namespace = PhpPsiUtil.getParentByCondition<PsiElement>(
                    this, true, PhpNamespace.INSTANCEOF
                ) as PhpNamespace?

                if (namespace != null && lastSlashIndex != -1) {
                    val startIndex = if (text.startsWith("\\\\")) 3 else 1
                    references.add(
                        PhpElementReference(
                            textElement,
                            namespace,
                            TextRange(startIndex, lastSlashIndex + 1)
                        )
                    )
                }
            }

            return references
        }

        private fun handleClassName(text: String, element: PsiElement): Array<PsiReference> {
            val references = mutableListOf<PsiReference>()

            val result = PhpDocLinkResolver.resolve(resolveName(text, element).toString(), element)
            result.forEach {
                val phpClass = it.phpClass
                if (phpClass != null) {
                    references.addAll(phpClass.getNamespaceAndClassReferences(element))
                } else {
                    references.addAll(it.element.getNamespaceAndClassReferences(element))
                }
            }

            if (references.isEmpty()) {
                return arrayOf(PhpUnknownElementReference(element))
            }

            return references.toTypedArray()
        }

        private fun handleMemberName(fqn: String, text: String, element: PsiElement): Array<PsiReference> {
            val references = mutableListOf<PsiReference>()

            var isMethodResolved = false
            val result = PhpDocLinkResolver.resolve(resolveName(fqn, element).toString(), element)
            result.forEach {
                val member = it.member ?: return@forEach

                val classEndIndex = text.indexOf("::")
                val klass = it.phpClass
                if (klass != null) {
                    references.addAll(klass.getNamespaceAndClassReferences(element, classEndIndex))
                }

                val startMethodIndex = classEndIndex + 1
                val range = TextRange(startMethodIndex + 1, text.length - 1)
                references.add(PhpElementReference(element, member, range))

                isMethodResolved = true
                return@forEach
            }

            if (!isMethodResolved) {
                return arrayOf(PhpUnknownElementReference(element))
            }

            return references.toTypedArray()
        }

        private fun handleComposerPackageName(packageName: String, element: PsiElement): Array<PsiReference> {
            val file = ComposerPackagesIndex.getInstance(element.project)
                .getPackageConfig(packageName)
                ?: return arrayOf(UnknownComposerPackageReference(element))

            val psiFile = PsiManager.getInstance(element.project).findFile(file)
                ?: return emptyArray()

            val jsonFile = psiFile as? JsonFile ?: return emptyArray()
            val nameKey = JsonUtil.getTopLevelObject(jsonFile)?.findProperty("name") ?: return emptyArray()
            val value = nameKey.value ?: return emptyArray()

            return arrayOf(PhpElementReference(element, value))
        }
    }

    class UnknownComposerPackageReference(element: PsiElement) : PsiReferenceBase<PsiElement?>(element) {
        override fun resolve(): PsiElement? = null

        override fun getRangeInElement() = TextRange(1, element.textLength - 1)

        override fun getVariants(): Array<Any> {
            val index = ComposerPackagesIndex.getInstance(element.project)
            val variants: MutableList<LookupElement> = mutableListOf()

            index.getPackages().forEach {
                variants.add(LookupElementBuilder.create(it.name))
            }

            return variants.toTypedArray()
        }
    }

    class PhpUnknownElementReference(element: PsiElement) : PsiReferenceBase<PsiElement?>(element) {
        override fun resolve(): PsiElement? = null
        override fun getRangeInElement() = TextRange(1, element.textLength - 1)
    }

    class PhpElementReference(
        private val myElement: PsiElement,
        private val myResult: PsiElement,
        private val myRange: TextRange? = null
    ) : PsiReference {

        override fun getElement() = myElement

        override fun getRangeInElement(): TextRange {
            if (myRange != null) {
                return myRange
            }
            val startOffset = 1
            return TextRange(startOffset, myElement.textLength - 1)
        }

        override fun resolve() = myResult

        override fun getCanonicalText(): String =
            if (myResult is PhpNamedElement) myResult.fqn else myElement.parent.text

        override fun handleElementRename(newElementName: String): PsiElement {
            val text = element.text
            val start = text.slice(1 until rangeInElement.startOffset)
            val end = text.slice(rangeInElement.endOffset until text.length)

            val textNode = YamlUtils.createQuotedText(myElement.project, start + newElementName + end)
            return myElement.replace(textNode)
        }

        override fun bindToElement(element: PsiElement) = throw UnsupportedOperationException()

        override fun isReferenceTo(element: PsiElement): Boolean = myResult === element

        override fun isSoft() = true
    }
}
