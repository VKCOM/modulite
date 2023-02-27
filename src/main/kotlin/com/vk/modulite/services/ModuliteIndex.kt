package com.vk.modulite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.vk.modulite.SymbolName
import com.vk.modulite.index.ModuliteFilesIndex
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.psi.extensions.files.psiFile
import com.vk.modulite.psi.extensions.yaml.moduliteName
import org.jetbrains.yaml.psi.YAMLFile
import java.util.*

@Service
class ModuliteIndex(private var project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<ModuliteIndex>()
        private val LOG = logger<ModuliteIndex>()
    }

    class CollectModulesProcessor(private val project: Project, private val mySearchFile: VirtualFile) : Processor<String> {
        var result: Modulite? = null
        private val allScope = GlobalSearchScope.allScope(project)

        override fun process(name: String): Boolean {
            val containingFiles = FileBasedIndex.getInstance().getContainingFiles(ModuliteFilesIndex.KEY, name, allScope)
            if (containingFiles.isEmpty()) {
                return true
            }

            if (containingFiles.any { it == mySearchFile }) {
                val modules = FileBasedIndex.getInstance().getValues(ModuliteFilesIndex.KEY, name, allScope)
                result = modules.firstOrNull().also {
                    it?.project = project
                }
            }

            return true
        }
    }

    fun getModulites(): List<Modulite> {
        val cached = CachedValuesManager.getManager(project).createCachedValue {
            val value = getModulitesImpl()
            CachedValueProvider.Result(value, ProjectRootModificationTracker.getInstance(project))
        }

        return if (true) {
            cached.value
        } else {
            getModulitesImpl()
        }
    }

    private fun getModulitesImpl(): List<Modulite> {
        val allScope = GlobalSearchScope.allScope(project)
        val keys = FileBasedIndex.getInstance().getAllKeys(ModuliteFilesIndex.KEY, project)
        val moduliteList = keys.map {
            FileBasedIndex.getInstance().getValues(ModuliteFilesIndex.KEY, it, allScope)
        }
        val modulites = moduliteList.flatten()
        modulites.forEach {
            modulitePostProcess(it)
        }
        return modulites
    }

    fun getConfigFile(name: String): VirtualFile? {
        val allScope = GlobalSearchScope.allScope(project)
        val containingFiles = FileBasedIndex.getInstance().getContainingFiles(ModuliteFilesIndex.KEY, name, allScope)
        return containingFiles.firstOrNull()
    }

    @Deprecated("Не использовать")
    fun getModulite(name: String): Modulite? {
        val allScope = GlobalSearchScope.allScope(project)
        val modulites = FileBasedIndex.getInstance().getValues(ModuliteFilesIndex.KEY, name, allScope)
        return modulites.firstOrNull()
            .also { modulitePostProcess(it) }
    }

    fun getModulite(name: String, composerPackageName: SymbolName): Modulite? {
        val allScope = GlobalSearchScope.allScope(project)
        val modulites = FileBasedIndex.getInstance()
            .getValues(ModuliteFilesIndex.KEY, name, allScope)
            .filter { it.containingPackage?.symbolName() == composerPackageName }
        return modulites.firstOrNull()
            .also { modulitePostProcess(it) }
    }

    private fun modulitePostProcess(it: Modulite?) {
        it?.project = project

//        val composerPackage = it?.containingPackage
//        if (composerPackage != null && !it.name.startsWith("#")) {
//            it.name = composerPackage.name + "/" + it.name
//        }
    }

    private fun getModuliteNormal(name: String, file: VirtualFile): Modulite? {
        val allScope = GlobalSearchScope.fileScope(project, file)
        val modulites = FileBasedIndex.getInstance().getValues(ModuliteFilesIndex.KEY, name, allScope)

        if (modulites.size > 1) {
            LOG.error("ОЛО!! блять")
        }

        return modulites.firstOrNull()
            .also { modulitePostProcess(it) }
    }


    fun getModuliteNormal(file: VirtualFile): Modulite? {
        val psiFile = file.psiFile<YAMLFile>(project)
        if (psiFile != null) {
            val name = psiFile.moduliteName() ?: ""
            if (name.isNotEmpty()) {
                return getModuliteNormal(name, file)
            }
        }

        LOG.error("Что-то пошло не так!!!")
        return null
    }
}
