package com.vk.modulite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.vk.modulite.index.TraitsIndex
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamespace
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.PhpUse
import com.jetbrains.php.lang.psi.elements.impl.PhpUseImpl

@Service(Service.Level.PROJECT)
class TraitIndexService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): TraitIndexService {
            return project.service()
        }
    }

    fun findTraitsByName(name: String): List<PhpClass> {
        val allScope = GlobalSearchScope.allScope(project)
        val traitFiles = FileBasedIndex.getInstance().getContainingFiles(TraitsIndex.KEY, name, allScope)
        val traits = mutableListOf<PhpClass>()

        traitFiles.forEach { file ->
            val psiFile = PsiManager.getInstance(project).findFile(file) as? PhpFile
            psiFile?.let {
                val classes = PsiTreeUtil.findChildrenOfType(it, PhpClass::class.java)
                classes.filter { klass -> klass.isTrait && klass.fqn == name }
                    .forEach { trait -> traits.add(trait) }
            }
        }

        return traits
    }

    fun getAllTraits(): List<PhpClass> {
        val allScope = GlobalSearchScope.allScope(project)
        val allKeys = FileBasedIndex.getInstance().getAllKeys(TraitsIndex.KEY, project)
        val traits = mutableListOf<PhpClass>()

        allKeys.forEach { key ->
            traits.addAll(findTraitsByName(key))
        }

        return traits
    }

    fun findTraitByFile(file: VirtualFile): PhpClass? {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? PhpFile
        psiFile?.let {
            val classes = PsiTreeUtil.findChildrenOfType(it, PhpClass::class.java)
            return classes.firstOrNull { klass -> klass.isTrait }
        }
        return null
    }

    fun getTraitsInFileOrder(file: VirtualFile): List<PhpClass> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? PhpFile ?: return emptyList()
        val traits = mutableListOf<PhpClass>()

        // Находим все use-операторы в файле
        val useStatements = PsiTreeUtil.findChildrenOfType(psiFile, PhpUse::class.java)

        useStatements.forEach { useStatement ->
            useStatement.children.forEach { child ->
                if (child is PhpReference) {
                    val resolvedTrait = child.resolve() as? PhpClass
                    if (resolvedTrait != null && resolvedTrait.isTrait) {
                        traits.add(resolvedTrait)
                    }
                }
            }
        }

        return traits
    }
}
