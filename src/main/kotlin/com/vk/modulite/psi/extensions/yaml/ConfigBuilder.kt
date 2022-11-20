package com.vk.modulite.psi.extensions.yaml

import com.intellij.openapi.project.Project
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.ModuliteRequires
import com.vk.modulite.utils.YamlUtils
import com.vk.modulite.utils.YamlUtils.addTopLevelKey
import com.vk.modulite.utils.YamlUtils.getTopLevelKey
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLSequence

object ConfigBuilder {
    val YAMLFile.namePsi get() = getTopLevelKey("name")
    val YAMLFile.exportPsi get() = getTopLevelKey("export")
    val YAMLFile.requirePsi get() = getTopLevelKey("require")
    val YAMLFile.forceInternalPsi get() = getTopLevelKey("force-internal")
    val YAMLFile.allowInternalAccessPsi get() = getTopLevelKey("allow-internal-access")

    fun YAMLFile.createAllowInternalAccess(place: String, seq: YAMLSequence) {
        addTopLevelKey(
            "allow-internal-access",
            YamlUtils.createMapping(place, seq)
        )
    }

    fun YAMLFile.replaceAllowInternalAccess(place: String, seq: YAMLSequence) {
        val node = YamlUtils.createKeyValue(
            "allow-internal-access",
            YamlUtils.createMapping(place, seq)
        )
        allowInternalAccessPsi?.replace(node)
    }

    fun YAMLFile.createExport(name: SymbolName) {
        addTopLevelKey("export", YamlUtils.createSequence(project, name.toYaml()))
    }

    fun YAMLFile.createExport(symbols: List<SymbolName>) {
        if (symbols.size == 1) {
            return createExport(symbols.first())
        }

        val seq = createSortedSymbolsSequence(project, symbols) ?: return
        addTopLevelKey("export", seq)
    }

    fun YAMLFile.createRequires(symbols: List<SymbolName>) {
        val seq = createSortedSymbolsSequence(project, symbols) ?: return
        addTopLevelKey("require", seq)
    }

    private fun createSortedSymbolsSequence(project: Project, symbols: List<SymbolName>): YAMLSequence? {
        val requires = ModuliteRequires(symbols)

        val sortedSymbols = mutableListOf<SymbolName>()
        sortedSymbols.addAll(requires.modulites().sortedBy { it.name })
        sortedSymbols.addAll(requires.composerPackages().sortedBy { it.name })
        sortedSymbols.addAll(requires.classes().sortedBy { it.name })
        sortedSymbols.addAll(requires.methods().sortedBy { it.name })
        sortedSymbols.addAll(requires.fields().sortedBy { it.name })
        sortedSymbols.addAll(requires.classConstants().sortedBy { it.name })
        sortedSymbols.addAll(requires.functions().sortedBy { it.name })
        sortedSymbols.addAll(requires.constants().sortedBy { it.name })
        sortedSymbols.addAll(requires.globalVariables().sortedBy { it.name })

        if (sortedSymbols.isEmpty()) {
            return null
        }

        return YamlUtils.createSequence(project, sortedSymbols)
    }
}
