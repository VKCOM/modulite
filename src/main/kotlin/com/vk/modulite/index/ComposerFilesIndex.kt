package com.vk.modulite.index

import com.intellij.json.psi.JsonFile
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.utils.fromKphpPolyfills
import com.vk.modulite.utils.fromPackages
import com.vk.modulite.utils.fromVendor
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Paths

class ComposerFilesIndex : FileBasedIndexExtension<String, ComposerPackage>() {
    override fun getIndexer(): DataIndexer<String, ComposerPackage, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, ComposerPackage>()

            val model = ComposerPackage.fromPsiFile(inputData.psiFile as JsonFile)
            map[model.name] = model

            map
        }
    }

    override fun getValueExternalizer(): DataExternalizer<ComposerPackage> {
        return object : DataExternalizer<ComposerPackage> {
            private val stringer = EnumeratorStringDescriptor.INSTANCE

            override fun save(out: DataOutput, value: ComposerPackage) {
                stringer.save(out, value.name)
                stringer.save(out, value.description)
                stringer.save(out, value.path.toString())
                stringer.save(out, value.namespace.toString())
                out.writeBoolean(value.moduliteEnabled)
                serializeSymbolNameList(out, value.exportList)
            }

            private fun serializeSymbolNameList(out: DataOutput, list: List<SymbolName>) {
                out.writeInt(list.size)
                list.forEach { stringer.save(out, it.toString()) }
            }

            override fun read(input: DataInput): ComposerPackage {
                val name = stringer.read(input)
                val description = stringer.read(input)
                val path = stringer.read(input)
                val namespace = stringer.read(input)
                val moduliteEnabled = input.readBoolean()
                val exportList = deserializeSymbolNameList(input)
                return ComposerPackage(name, description, Paths.get(path), Namespace(namespace), exportList, moduliteEnabled)
            }

            private fun deserializeSymbolNameList(input: DataInput): List<SymbolName> {
                val publicSize = input.readInt()
                val public = ArrayList<SymbolName>(publicSize)
                for (i in 0 until publicSize) {
                    public.add(SymbolName(stringer.read(input)))
                }
                return public
            }
        }
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file ->
            if (file.name != "composer.json") {
                return@InputFilter false
            }

            // Перенести проверку в логику
            if (file.fromKphpPolyfills()) {
                return@InputFilter false
            }

            // Не оптимальный вариант.
            // На самом деле это защита, он индексирования composer текущего проекта.
            return@InputFilter file.fromVendor() || file.fromPackages()
        }
    }

    override fun getName() = KEY
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true
    override fun getVersion() = 20

    companion object {
        val KEY = ID.create<String, ComposerPackage>("modulite.composer.config.file")
    }
}
