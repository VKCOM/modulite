package com.vk.modulite.modulite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
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
        val filePath = if (application.isUnitTestMode) {
            VirtualFileManager.getInstance().findFileByUrl("temp://$path")
        } else {
            LocalFileSystem.getInstance().findFileByPath(path)
        }

        // Если это симлинка, то вернём путь до оригинального файла
        // Это нужно для пакетов, которые лежат в текущем проекте
        // vendor/vendor_name/project_name -> packages/vendor_name/project_name
        return filePath?.canonicalFile
    }

    protected inline fun <reified T : PsiFile> configPsiFileImpl(): T? {
        val file = configFile() ?: return null
        return file.psiFile(project)
    }

    fun isExport(element: PhpNamedElement, reference: PhpReference? = null): Boolean {
        // Пустой export в 'composer пакетах' обозначает, что все символы общедоступные
        if (this.name == "<composer_root>" && this.exportList.isEmpty()) {
            return true
        }

        val name = element.symbolName(reference)
        if (name.isClassMember()) {
            val isInternal = forceInternalList.any { internal ->
                name.equals(internal, this)
            }
            // Если он есть в списке internal, то он не публичен
            if (isInternal) return false

            // В ином случае нам нужно проверить публичен ли его класс или он сам
            val className = name.className()
            return exportList.any { public ->
                className.equals(public, this) || name.equals(public, this)
            }
        }

        val isExported = exportList.any { export ->
            name.equals(export, this)
        }
        if (isExported) return true

        if (name.kind == SymbolName.Kind.Class) {
            // TODO: добавить описание
            val methodDeclaration = reference?.parent as? MethodReference ?: return false
            val methodElement = methodDeclaration.resolve() as? MethodImpl ?: return false

            return isExport(methodElement, methodDeclaration)
        }

        return false
    }
}
