package com.vk.modulite.psi.extensions.json

import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.util.PsiTreeUtil
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.psi.ModuliteNamePsi
import com.vk.modulite.services.ComposerPackagesIndex
import com.vk.modulite.utils.unquote

val JsonFile.namePsi get() = getTopLevelKey("name")

fun JsonFile.composerPackageName(): String? {
    val name = namePsi ?: return null
    return name.value?.text?.unquote()
}

fun JsonFile.composerPackageNamePsi(): ModuliteNamePsi? {
    val name = namePsi ?: return null
    return name.value as? ModuliteNamePsi
}

fun JsonFile.getTopLevelKey(name: String): JsonProperty? {
    return getPropertiesValues().find { it.name == name }
}

fun JsonFile.getPropertiesValues(): List<JsonProperty> {
    val obj = JsonUtil.getTopLevelObject(this) ?: return emptyList()
    return PsiTreeUtil.getChildrenOfTypeAsList(obj, JsonProperty::class.java)
}

fun JsonStringLiteral.composerPackage(): ComposerPackage? =
    ComposerPackagesIndex.getInstance(project).getPackage("#" + text.unquote())
