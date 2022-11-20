package com.vk.modulite.psi.extensions.yaml

import com.intellij.psi.PsiNamedElement
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLQuotedText

fun YAMLPsiElement.containingModulite(): Modulite? {
    val virtualFile = containingFile.virtualFile ?: return null
    return ModuliteIndex.getInstance(project).getModulite(virtualFile)
}

fun YAMLQuotedText.resolveSymbolName(): SymbolName? {
    val name = text.unquote()
    // In requires we have FQN.
    if (YamlUtils.insideRequires(this)) {
        return SymbolName(name, fromYaml = true)
    }

    // For defines we have also FQN
    // TODO: улучшить понимание что это константа
    if (name.startsWith('\\') && name == name.uppercase()) {
        return SymbolName(name, kind = SymbolName.Kind.Constant, fromYaml = true)
    }

    // In other cases, we have a relative name.
    val rawName = SymbolName(name, fromYaml = true) // kind will be inferred from the text

    // Special names don't need to be resolved.
    if (rawName.kind == SymbolName.Kind.GlobalVariable ||
        rawName.kind == SymbolName.Kind.Modulite ||
        rawName.kind == SymbolName.Kind.ComposerPackage
    ) {
        return rawName
    }

    val modulite = containingFile.containingModulite() ?: return null
    return rawName.absolutize(modulite.namespace)
}

fun YAMLQuotedText.resolveSymbol(): List<PsiNamedElement> {
    val symbolName = resolveSymbolName() ?: return emptyList()
    return symbolName.resolve(project)
}
