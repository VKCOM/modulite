package com.vk.modulite.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLParserDefinition

class ModuliteYamlParserDefinition : YAMLParserDefinition() {
    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            YAMLElementTypes.SCALAR_QUOTED_STRING -> {
                if (node.text.unquote().startsWith("@")) {
                    ModuliteNamePsi(node)
                } else {
                    super.createElement(node)
                }
            }
            else -> super.createElement(node)
        }
    }
}
