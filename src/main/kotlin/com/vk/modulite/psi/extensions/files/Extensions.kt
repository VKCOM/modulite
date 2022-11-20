package com.vk.modulite.psi.extensions.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ContentIteratorEx
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.php.lang.psi.elements.MemberReference
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.services.ComposerPackagesIndex
import com.vk.modulite.services.ModuliteIndex

fun VirtualFile.containingModulite(project: Project): Modulite? {
    if (name == ".modulite.yaml") {
        return ModuliteIndex.getInstance(project).getModulite(this)
    }

    return containingModulite(project, ModuliteIndex.getInstance(project).getModulites())
}

fun VirtualFile.containingModulite(project: Project, modulites: List<Modulite>): Modulite? {
    if (name == ".modulite.yaml") {
        return ModuliteIndex.getInstance(project).getModulite(this)
    }

    val containsModulites = modulites.filter { it.contains(this) }

    var deepestModulite: Modulite? = null

    containsModulites.forEach {
        if (it.path.length > (deepestModulite?.path?.length ?: 0)) {
            deepestModulite = it
        }
    }

    return deepestModulite
}

inline fun <reified T : PsiFile> VirtualFile.psiFile(project: Project): T? {
    val file = PsiManager.getInstance(project).findFile(this) ?: return null
    return if (file is T) file else null
}

fun VirtualFile.forEachFiles(project: Project, iterator: ContentIterator) =
    ProjectFileIndex.getInstance(project).iterateContentUnderDirectory(this, iterator)

@Suppress("UnstableApiUsage")
fun VirtualFile.forEachFilesEx(project: Project, iterator: ContentIteratorEx) =
    ProjectFileIndex.getInstance(project).iterateContentUnderDirectory(this, iterator)

fun PsiFile.containingModulite(): Modulite? {
    return virtualFile?.containingModulite(project)
}

fun PsiDirectory.containingModulite(): Modulite? {
    return virtualFile.containingModulite(project)
}

fun PsiElement.containingModulite(): Modulite? {
    if (this is Variable) {
        return null
    }

    val file = containingFile?.virtualFile
    return file?.containingModulite(project)
}

fun PsiElement.containingModulite(reference: PhpReference): Modulite? {
    if (this is Variable) {
        return null
    }

    // При наследовании методы / поля / константы резолвятся на класс в котором они были описаны
    if (reference is MemberReference) {
        val klass = reference.classReference?.reference?.resolve()
        return klass?.containingModulite()
    }

    val file = containingFile?.virtualFile
    return file?.containingModulite(project)
}

fun VirtualFile.containingComposerPackage(project: Project): ComposerPackage? {
    return containingComposerPackage(project, ComposerPackagesIndex.getInstance(project).getPackages())
}

fun VirtualFile.containingComposerPackage(project: Project, packages: List<ComposerPackage>): ComposerPackage? {
    if (name == "composer.json") {
        return ComposerPackagesIndex.getInstance(project).getPackage(this)
    }
    return packages.find {
        it.contains(this)
    }
}

fun PsiFile.containingComposerPackage(): ComposerPackage? {
    return virtualFile?.containingComposerPackage(project)
}

fun PsiElement.containingComposerPackage(): ComposerPackage? {
    val file = containingFile?.virtualFile
    return file?.containingComposerPackage(project)
}
