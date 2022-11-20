package com.vk.modulite.modulite

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.vk.modulite.notifications.ModuliteNotification
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.yaml.diffDependencies
import com.vk.modulite.services.ModuliteDependenciesCollector
import com.vk.modulite.services.ModuliteDepsDiff
import com.vk.modulite.utils.Utils.runBackground
import com.vk.modulite.utils.Utils.runModal

object ModuliteDependenciesManager {
    fun regenerate(project: Project, configFile: VirtualFile) {
        val moduliteDir = configFile.parent ?: return
        val modulite = configFile.containingModulite(project) ?: return

        runModal(project, "Regenerate ${modulite.name} requires") {
            val deps = runReadAction {
                ModuliteDependenciesCollector.getInstance(project).collect(moduliteDir).forModulite(modulite)
            }

            invokeLater {
                modulite.replaceDependencies(deps) { diff ->
                    if (diff.isEmpty()) {
                        ModuliteNotification("No changes")
                            .withTitle("${modulite.name} requires regenerated")
                            .show()
                        return@replaceDependencies
                    }

                    ModuliteNotification(diff.shortInfo())
                        .withTitle("${modulite.name} requires regenerated")
                        .withActions(ModuliteNotification.Action("Show details...") { _, _ ->
                            diff.showDetails()
                        })
                        .show()
                }
            }
        }
    }

    fun actualRequiresDiff(project: Project, modulite: Modulite, onReady: (ModuliteDepsDiff?) -> Unit) {
        val moduliteDir = modulite.directory() ?: return

        runBackground(project, "Calculate ${modulite.name} requires") {
            runReadAction {
                val deps = ModuliteDependenciesCollector.getInstance(project).collect(moduliteDir).forModulite(modulite)
                val diff = modulite.configPsiFile()?.diffDependencies(deps)

                onReady(diff)
            }
        }
    }
}
