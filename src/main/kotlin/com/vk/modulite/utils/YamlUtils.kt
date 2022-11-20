package com.vk.modulite.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.vk.modulite.SymbolName
import com.vk.modulite.utils.Utils.writeCommand
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.*

object YamlUtils {
    fun YAMLFile.getTopLevelKey(name: String): YAMLKeyValue? {
        return YAMLUtil.getTopLevelKeys(this).find { it.key?.text == name }
    }

    fun YAMLFile.addTopLevelKey(name: String, value: YAMLPsiElement): YAMLKeyValue? {
        val topLevelValue = (documents[0] as YAMLDocument).topLevelValue
        if (topLevelValue !is YAMLMapping) return null

        val node = createKeyValue(name, value)
        topLevelValue.putKeyValue(node)
        return node
    }

    fun createKeyValue(name: String, value: YAMLPsiElement): YAMLKeyValue {
        val keyValue = YAMLElementGenerator.getInstance(value.project)
            .createYamlKeyValue(name, "- empty")

        keyValue.value?.replace(value)

        return keyValue
    }

    fun YAMLSequence.addElement(project: Project, value: String) = writeCommand(project) {
        val alreadyContains = items.find { it.value?.text?.unquote() == value } != null
        if (alreadyContains) {
            return@writeCommand
        }

        val seqItem = createSequenceItem(project, value)

        var eol = YAMLElementGenerator.getInstance(project).createEol()

        if (lastChild != null) {
            eol = addAfter(eol, lastChild)
            addAfter(seqItem, eol)
        } else {
            add(seqItem)
        }
    }

    fun YAMLSequence.removeElement(element: String) {
        removeElements(setOf(element))
    }

    fun YAMLSequence.removeElements(elements: List<String>) {
        removeElements(elements.toSet())
    }

    fun YAMLSequence.removeElements(elements: Set<String>) {
        val countChildren = items.size
        var child: PsiElement? = firstChild
        var index = 0
        while (child != null) {
            if (child is YAMLSequenceItem) {
                index++
                val text = child.value?.text?.unquote()
                if (elements.contains(text)) {
                    val next = child.nextSibling
                    if (next != null && next.node.elementType == YAMLTokenTypes.EOL) {
                        next.delete()
                    }

                    val temp = child.nextSibling
                    child.delete()
                    child = temp

                    if (index == countChildren) {
                        val afterList = parent?.nextSibling
                        if (afterList != null && afterList.node.elementType == YAMLTokenTypes.EOL) {
                            afterList.delete()
                        }
                    }

                    continue
                }
            }
            child = child.nextSibling
        }
    }

    private fun createSequenceItem(project: Project, content: String) = PsiTreeUtil.findChildOfType(
        YAMLElementGenerator.getInstance(project).createDummyYamlWithText("- \"$content\""),
        YAMLSequenceItem::class.java
    )!!

    fun createSequence(project: Project, content: String) = PsiTreeUtil.findChildOfType(
        YAMLElementGenerator.getInstance(project).createDummyYamlWithText("- \"$content\""),
        YAMLSequence::class.java
    )!!

    fun createSequence(project: Project, elements: List<SymbolName>): YAMLSequence {
        val content = elements.joinToString("\n") { "- \"${it.toYaml()}\"" }

        return PsiTreeUtil.findChildOfType(
            YAMLElementGenerator.getInstance(project).createDummyYamlWithText(content),
            YAMLSequence::class.java
        )!!
    }

    fun createMapping(project: Project, key: String) = PsiTreeUtil.findChildOfType(
        YAMLElementGenerator.getInstance(project).createDummyYamlWithText(
            "$key: \"\"\n"
        ),
        YAMLMapping::class.java
    )!!

    fun createMapping(key: String, value: YAMLPsiElement): YAMLMapping {
        val mapping = createMapping(value.project, key.quote())
        val keyValue = mapping.getKeyValueByKey(key)

        keyValue?.value?.replace(value)

        return mapping
    }

    fun createQuotedText(project: Project, content: String) = PsiTreeUtil.findChildOfType(
        YAMLElementGenerator.getInstance(project).createDummyYamlWithText("\"$content\""),
        YAMLQuotedText::class.java
    )!!

    fun insideName(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "name") != null
    }

    fun insideNamespace(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "namespace") != null
    }

    fun insideDescription(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "description") != null
    }

    fun insideExport(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "export") != null
    }

    fun insideForceInternal(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "force-internal") != null
    }

    fun insideRequires(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "require") != null
    }

    fun insideAllowInternalAccess(element: PsiElement): Boolean {
        return getParentKeyValueWithName(element, "allow-internal-access") != null
    }

    fun getParentKeyValue(element: PsiElement, comparator: (YAMLKeyValue) -> Boolean): YAMLKeyValue? {
        var parent = element as PsiElement?
        while (parent != null) {
            if (parent is YAMLKeyValue && comparator(parent)) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    fun getParentKeyValueWithName(element: PsiElement, name: String): YAMLKeyValue? {
        return getParentKeyValue(element) { it.key?.text?.unquote() == name }
    }
}
