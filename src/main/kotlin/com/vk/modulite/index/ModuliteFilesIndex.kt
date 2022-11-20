package com.vk.modulite.index

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteRequires
import gnu.trove.THashMap
import org.jetbrains.yaml.psi.YAMLFile
import java.io.DataInput
import java.io.DataOutput

class ModuliteFilesIndex : FileBasedIndexExtension<String, Modulite>() {
    override fun getIndexer(): DataIndexer<String, Modulite, FileContent> {
        return DataIndexer { inputData ->
            val map = THashMap<String, Modulite>()

            val model = Modulite.fromYamlPsi(inputData.psiFile as YAMLFile)
            map[model.name] = model

            map
        }
    }

    override fun getValueExternalizer(): DataExternalizer<Modulite> {
        return object : DataExternalizer<Modulite> {
            private val stringer = EnumeratorStringDescriptor.INSTANCE

            override fun save(out: DataOutput, value: Modulite) {
                stringer.save(out, value.name)
                stringer.save(out, value.description)
                stringer.save(out, value.path)
                stringer.save(out, value.namespace.toString())

                serializeSymbolNameList(out, value.exportList)
                serializeSymbolNameList(out, value.forceInternalList)

                serializeFqnList(out, value.requires.symbols)

                out.writeInt(value.allowedInternalAccess.size)
                value.allowedInternalAccess.forEach {
                    stringer.save(out, it.key.toString())
                    serializeSymbolNameList(out, it.value)
                }
            }

            private fun serializeSymbolNameList(out: DataOutput, list: List<SymbolName>) {
                out.writeInt(list.size)
                list.forEach { stringer.save(out, it.toString()) }
            }

            private fun serializeFqnList(out: DataOutput, list: List<SymbolName>) {
                out.writeInt(list.size)
                list.forEach {
                    out.writeInt(it.kind.ordinal)
                    stringer.save(out, it.toString())
                }
            }

            override fun read(input: DataInput): Modulite {
                val name = stringer.read(input)
                val description = stringer.read(input)
                val path = stringer.read(input)
                val namespace = stringer.read(input)

                val public = deserializeSymbolNameList(input)
                val internal = deserializeSymbolNameList(input)

                val symbols = deserializeFqnList(input)

                val allowedMapSize = input.readInt()
                val allowedInternalAccess = LinkedHashMap<SymbolName, List<SymbolName>>(allowedMapSize)

                for (i in 0 until allowedMapSize) {
                    val key = stringer.read(input)
                    val list = deserializeSymbolNameList(input)
                    val symbolName = SymbolName(key)

                    allowedInternalAccess[symbolName] = list
                }

                return Modulite(
                    name, description,
                    path, Namespace(namespace),
                    ModuliteRequires(symbols),
                    public,
                    internal,
                    allowedInternalAccess
                )
            }

            private fun deserializeSymbolNameList(input: DataInput): List<SymbolName> {
                val publicSize = input.readInt()
                val public = ArrayList<SymbolName>(publicSize)
                for (i in 0 until publicSize) {
                    public.add(SymbolName(stringer.read(input)))
                }
                return public
            }

            private fun deserializeFqnList(input: DataInput): List<SymbolName> {
                val publicSize = input.readInt()
                val public = ArrayList<SymbolName>(publicSize)
                for (i in 0 until publicSize) {
                    val type = input.readInt()
                    val text = stringer.read(input)

                    val kind = SymbolName.Kind.values()[type]
                    val name = SymbolName(text, kind = kind)

                    public.add(name)
                }
                return public
            }
        }
    }

    override fun getInputFilter() = FileBasedIndex.InputFilter { file -> file.name == ".modulite.yaml" }
    override fun getName() = KEY
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true
    override fun getVersion() = 21

    companion object {
        val KEY = ID.create<String, Modulite>("modulite.config.file")
    }
}
