package com.vk.modulite.actions.usages.base

import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.DefaultSearchScopeProviders
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.SearchScopeProvider
import com.intellij.psi.search.scope.packageSet.FilteredPackageSet
import com.intellij.psi.search.scope.packageSet.NamedScope
import com.intellij.psi.search.scope.packageSet.NamedScopeManager
import com.intellij.util.SlowOperations
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.notifications.ModuliteWarningNotification
import java.io.File

abstract class UsagesBaseFinder {
    companion object {
        private val LOG = logger<UsagesBaseFinder>()
    }

    protected class AbortActionException : Exception()

    abstract fun resolveSearchElement(element: PsiElement): PsiElement?
    abstract fun resolveSearchModulite(element: PsiElement): Modulite?

    fun find(element: PsiElement) {
        val searchModulite = resolveSearchModulite(element)
        if (searchModulite == null) {
            LOG.warn("Modulite to search is null")
            ModuliteWarningNotification("Perhaps the modulite to search in does not exist")
                .withTitle("Modulite not found")
                .show()
            return
        }
        val searchElement = try {
            resolveSearchElement(element)
        } catch (e: AbortActionException) {
            LOG.warn("Abort find usages action")
            return
        }
        if (searchElement == null) {
            LOG.warn("Search element is null")
            ModuliteWarningNotification("Perhaps search element does not exist")
                .withTitle("Search element not found")
                .show()
            return
        }

        addModuleScope(element.project, searchModulite)

        val provider = SearchScopeProvider.EP_NAME.extensions.find {
            it is DefaultSearchScopeProviders.CustomNamed
        } ?: return

        val scopes = SlowOperations.allowSlowOperations<List<SearchScope>, RuntimeException> {
            provider.getSearchScopes(
                element.project,
                createContext(element.project, element)
            )
        }

        val scope = scopes.find {
            it.displayName == searchModulite.name
        } ?: return

        val findManager = FindManager.getInstance(element.project) as? FindManagerImpl ?: return
        val findUsagesManager = findManager.findUsagesManager

        findUsagesManager.findUsages(
            searchElement,
            null,
            null,
            false,
            scope,
        )
    }

    private fun createContext(project: Project, context: PsiElement): DataContext {
        val parentContext = SimpleDataContext.getProjectContext(project)
        val file = context.containingFile
        return SimpleDataContext.builder()
            .setParent(parentContext)
            .add(CommonDataKeys.PSI_ELEMENT, context)
            .add(CommonDataKeys.PSI_FILE, file)
            .build()
    }

    private fun addModuleScope(project: Project, modulite: Modulite) {
        val scopes = NamedScopeManager.getInstance(project).scopes
        val scope = createModuliteScope(modulite)
        val newScopes = scopes.filter { it.scopeId != modulite.name }.toMutableList()
        newScopes.add(scope)
        NamedScopeManager.getInstance(project).scopes = newScopes.toTypedArray()
    }

    private fun createModuliteScope(modulite: Modulite): NamedScope {
        val name = modulite.name
        val children = modulite.children()
        return NamedScope(
            name,
            { name },
            AllIcons.Ide.LocalScope,
            object : FilteredPackageSet("${name}_files") {
                private val moduliteFolder = File(modulite.path).parent
                private val excludedFolders = children.map { File(it.path).parent }

                override fun contains(file: VirtualFile, project: Project): Boolean {
                    val contains = file.path.startsWith(moduliteFolder)
                    if (!contains) {
                        return false
                    }
                    return !excludedFolders.any { file.path.startsWith(it) }
                }
            }
        )
    }
}
