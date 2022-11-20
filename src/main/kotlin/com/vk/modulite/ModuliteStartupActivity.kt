package com.vk.modulite

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.spellchecker.SpellCheckerManager

class ModuliteStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        setupWordsForSpellChecker(project)
    }

    private fun setupWordsForSpellChecker(project: Project) {
        val manager = SpellCheckerManager.getInstance(project)
        manager.acceptWordAsCorrect("modulite", project)
        manager.acceptWordAsCorrect("modulites", project)
    }
}
