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
import java.nio.file.Path
import java.nio.file.Paths

data class ComposerPackage(
    override val name: String,
    override val description: String,
    override val path: Path,
    override val namespace: Namespace,
    override val exportList: List<SymbolName>,
    val moduliteEnabled: Boolean,
) : ModuliteBase() {

    private val symbolName = SymbolName(name, SymbolName.Kind.ComposerPackage)

    override val forceInternalList: List<SymbolName> = emptyList()

    fun contains(file: VirtualFile): Boolean {
        val moduliteFolder = path.parent
        return Paths.get(file.path).startsWith(moduliteFolder)
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
        fun fromPsiFile(file: JsonFile): ComposerPackage {
            val values = file.getPropertiesValues()
            var name = ""
            var description = ""
            var exportList = emptyList<SymbolName>()
            var namespace = ""
            var moduliteEnabled = false
            values.forEach {
                when (it.name) {
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

            return ComposerPackage(
                "#$name",
                description,
                Paths.get(file.virtualFile.path),
                Namespace(namespace),
                exportList,
                moduliteEnabled,
            )
        }
    }
}
