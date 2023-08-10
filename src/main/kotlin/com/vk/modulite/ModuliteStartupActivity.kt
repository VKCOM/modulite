package com.vk.modulite

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.spellchecker.SpellCheckerManager

class ModuliteStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        setupWordsForSpellChecker(project)
    }

    private fun setupWordsForSpellChecker(project: Project) {
        val manager = SpellCheckerManager.getInstance(project)
        manager.acceptWordAsCorrect("modulite", project)
        manager.acceptWordAsCorrect("modulites", project)
    }
}
