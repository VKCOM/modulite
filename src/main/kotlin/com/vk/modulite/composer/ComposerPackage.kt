package com.vk.modulite.composer

import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.vfs.VirtualFile
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.ModuliteBase
import com.vk.modulite.psi.extensions.json.composerPackageNamePsi
import com.vk.modulite.psi.extensions.json.getPropertiesValues
import com.vk.modulite.utils.unquote
import java.io.File

data class ComposerPackage(
    override val name: String,
    override val description: String,
    override val path: String,
    override val namespace: Namespace,
    override val exportList: List<SymbolName>,
    val moduliteEnabled: Boolean,
) : ModuliteBase() {

    private val symbolName = SymbolName(name, SymbolName.Kind.ComposerPackage)

    override val forceInternalList: List<SymbolName> = emptyList()

    fun contains(file: VirtualFile): Boolean {
        val moduliteFolder = File(path).parent
        return file.path.startsWith(moduliteFolder + File.separator)
    }

    fun getPackageDir(): VirtualFile? {
        val file = configFile() ?: return null
        return file.parent
    }

    fun namePsi() = configPsiFile()?.composerPackageNamePsi()

    fun configPsiFile(): JsonFile? = super.configPsiFileImpl()

    override fun symbolName() = symbolName

    override fun toString() = name

    companion object {
        fun fromPsiFile(file: JsonFile): ComposerPackage? {
            val values = file.getPropertiesValues()
            var name = ""
            var description = ""
            var isPackage = false
            var exportList = emptyList<SymbolName>()
            var namespace = ""
            var moduliteEnabled = false
            values.forEach {
                when (it.name) {
                    "type" -> {
                        isPackage = it.value?.text?.unquote() == "library"
                    }
                    "name" -> {
                        name = it.value?.text?.unquote() ?: ""
                    }
                    "description" -> {
                        description = it.value?.text?.unquote() ?: ""
                    }
                    "modulite" -> {
                        moduliteEnabled = true

                        val propValue = it.value as? JsonObject ?: return@forEach

                        propValue.propertyList.forEach { prop ->
                            when (prop.name) {
                                "namespace" -> {
                                    namespace = prop.value?.text?.unquote() ?: ""
                                }
                                "export" -> {
                                    val exportListPsi = prop?.value as? JsonArray
                                    exportList = exportListPsi?.valueList?.map { value ->
                                        SymbolName(value.text.unquote(), fromYaml = true)
                                    } ?: emptyList()
                                }
                                else -> {}
                            }
                        }

                    }
                }
            }
            if (!isPackage) {
                return null
            }
            return ComposerPackage(
                "#$name",
                description,
                file.virtualFile.path,
                Namespace(namespace),
                exportList,
                moduliteEnabled,
            )
        }
    }
}
