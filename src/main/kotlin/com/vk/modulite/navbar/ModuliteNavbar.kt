package com.vk.modulite.navbar

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDirectory
import com.jetbrains.php.lang.PhpLanguage
import com.vk.modulite.psi.extensions.files.containingModulite

class ModuliteNavbar : StructureAwareNavBarModelExtension() {
    override val language: Language = PhpLanguage.INSTANCE

    override fun getPresentableText(obj: Any): String? {
        if (obj !is PsiDirectory) {
            return null
        }

        // If the indexing process is in progress.
        if (obj.project.service<DumbService>().isDumb) return null

        val config = obj.findFile(".modulite.yaml") ?: return null
        val modulite = config.containingModulite() ?: return null

        return obj.name + " (${modulite.foldedName()})"
    }
}
