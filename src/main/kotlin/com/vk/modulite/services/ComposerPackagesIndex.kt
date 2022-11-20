package com.vk.modulite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.index.ComposerFilesIndex

@Service
class ComposerPackagesIndex(private var project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<ComposerPackagesIndex>()
    }

    class CollectModulesProcessor(
        project: Project,
        private val mySearchFile: VirtualFile,
    ) : Processor<String> {

        private val allScope = GlobalSearchScope.allScope(project)
        var result: ComposerPackage? = null

        override fun process(name: String): Boolean {
            val containingFiles =
                    FileBasedIndex.getInstance().getContainingFiles(ComposerFilesIndex.KEY, name, allScope)
            if (containingFiles.isEmpty()) {
                return true
            }

            if (containingFiles.any { it == mySearchFile }) {
                val modules = FileBasedIndex.getInstance().getValues(ComposerFilesIndex.KEY, name, allScope)
                result = modules.firstOrNull()
            }

            return true
        }
    }

    fun getPackages(): List<ComposerPackage> {
        val allScope = GlobalSearchScope.allScope(project)
        val keys = FileBasedIndex.getInstance().getAllKeys(ComposerFilesIndex.KEY, project)
        val modules = keys.map {
            FileBasedIndex.getInstance().getValues(ComposerFilesIndex.KEY, it, allScope)
        }
        return modules.flatten()
            .map {
                it?.project = project
                it
            }
    }

    fun getPackageConfig(name: String): VirtualFile? {
        val allScope = GlobalSearchScope.allScope(project)
        val containingFiles = FileBasedIndex.getInstance().getContainingFiles(ComposerFilesIndex.KEY, name, allScope)
        return containingFiles.firstOrNull()
    }

    fun getPackage(name: String): ComposerPackage? {
        val allScope = GlobalSearchScope.allScope(project)
        val modules = FileBasedIndex.getInstance().getValues(ComposerFilesIndex.KEY, name, allScope)
        return modules.firstOrNull()
            .also { it?.project = project }
    }

    fun getPackage(file: VirtualFile): ComposerPackage? {
        val processor = CollectModulesProcessor(project, file)
        FileBasedIndex.getInstance().processAllKeys(ComposerFilesIndex.KEY, processor, project)
        return processor.result
            .also { it?.project = project }
    }
}
