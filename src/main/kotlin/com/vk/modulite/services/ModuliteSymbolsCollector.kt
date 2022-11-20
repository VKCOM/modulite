package com.vk.modulite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIteratorEx
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.vk.modulite.SymbolName
import com.vk.modulite.psi.PhpRecursiveElementVisitor
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.files.forEachFilesEx
import com.vk.modulite.psi.extensions.files.psiFile
import com.vk.modulite.psi.extensions.php.symbolName

/**
 * Собирает все символы определенные в текущей папке и всех подпапках.
 */
@Service
@Suppress("UnstableApiUsage")
class ModuliteSymbolsCollector(val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<ModuliteSymbolsCollector>()
        private val LOG = logger<ModuliteDependenciesCollector>()
    }

    fun collect(dir: VirtualFile): List<SymbolName> {
        val result = mutableListOf<SymbolName>()
        val moduliteConfig = dir.findChild(".modulite.yaml")

        dir.forEachFilesEx(project) files@{ file ->
            // Проверяем не отменил ои пользователь операцию.
            ProgressManager.checkCanceled()

            if (ModuliteDependenciesCollector.isInnerModulite(file, moduliteConfig)) {
                val innerModuliteConfig = file.findChild(".modulite.yaml")!!
                val modulite = innerModuliteConfig.containingModulite(project) ?: return@files ContentIteratorEx.Status.CONTINUE
                result.add(modulite.symbolName())

                return@files ContentIteratorEx.Status.SKIP_CHILDREN
            }

            val psiFile = file.psiFile<PhpFile>(project) ?: return@files ContentIteratorEx.Status.CONTINUE

            psiFile.accept(object : PhpRecursiveElementVisitor() {
                override fun visitPhpClass(element: PhpClass) {
                    result.add(element.symbolName())
                }

                override fun visitPhpFunction(element: Function) {
                    result.add(element.symbolName())
                }

                override fun visitPhpConstant(element: Constant) {
                    result.add(element.symbolName())
                }
            })

            ContentIteratorEx.Status.CONTINUE
        }

        return result
    }
}
