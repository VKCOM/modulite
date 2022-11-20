package com.vk.modulite.modulite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.psi.extensions.files.psiFile
import com.vk.modulite.psi.extensions.php.symbolName

/**
 * Base class for [Modulite] ans [ComposerPackage].
 */
abstract class ModuliteBase {
    lateinit var project: Project

    abstract val name: String
    abstract val description: String
    abstract val path: String
    abstract val namespace: Namespace
    abstract val exportList: List<SymbolName>
    abstract val forceInternalList: List<SymbolName>

    abstract fun symbolName(): SymbolName

    fun configFile(): VirtualFile? {
        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode) {
            return VirtualFileManager.getInstance().findFileByUrl("temp://$path")
        }

        return LocalFileSystem.getInstance().findFileByPath(path)
    }

    protected inline fun <reified T : PsiFile> configPsiFileImpl(): T? {
        val file = configFile() ?: return null
        return file.psiFile(project)
    }

    fun isExport(element: PhpNamedElement, reference: PhpReference? = null): Boolean {
        val name = element.symbolName(reference)
        if (name.isClassMember()) {
            val isInternal = forceInternalList.any { internal ->
                name.equals(internal, this)
            }
            // Если он есть в списке internal, то он не публичен
            if (isInternal) return false

            // В ином случае нам нужно проверить публичен ли класс
            val className = name.className()
            return exportList.any { public ->
                className.equals(public, this)
            }
        }

        return exportList.any { export ->
            name.equals(export, this)
        }
    }
}
