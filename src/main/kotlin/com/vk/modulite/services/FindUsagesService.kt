package com.vk.modulite.services

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference

@Service
class FindUsagesService(val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<FindUsagesService>()
        private val LOG = logger<FindUsagesService>()
    }

    fun findUsages(element: PhpNamedElement): List<PhpReference> {
        runReadAction { LOG.info("Searching for usages of ${element.name}") }

        val query = ReferencesSearch.search(element)
        return query.toList().mapNotNull { it as? PhpReference }
    }
}
