package com.vk.modulite.settings


import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

class ModuliteSettingsConfigurable : Configurable {
    private val mainPanel: DialogPanel
    private val model = ModuliteSettingsState()
    private val settings = ModuliteSettings.getInstance()

    init {
        mainPanel = panel {
            group("General") {
                row {
                    checkBox("Turn off modulite icon on folders")
                        .comment("Can improve indexing performance.")
                        .bindSelected(model::turnOffIconsOnFolders)
                }
                row {
                    checkBox("Turn off modulite icon on yaml file")
                        .comment("Can improve indexing performance.")
                        .bindSelected(model::turnOffIconOnYaml)
                }
            }

        }
    }

    override fun getDisplayName() = "Modulite"
    override fun getPreferredFocusedComponent() = mainPanel
    override fun createComponent() = mainPanel

    override fun isModified(): Boolean {
        mainPanel.apply()

        return model.turnOffIconsOnFolders != settings.state.turnOffIconsOnFolders ||
                model.turnOffIconOnYaml != settings.state.turnOffIconOnYaml
    }

    override fun apply() {
        mainPanel.apply()

        with(settings) {
            state.turnOffIconsOnFolders = model.turnOffIconsOnFolders
            state.turnOffIconOnYaml = model.turnOffIconOnYaml
        }
    }

    override fun reset() {
        with(model) {
            turnOffIconsOnFolders = settings.state.turnOffIconsOnFolders
            turnOffIconOnYaml = settings.state.turnOffIconOnYaml
        }

        mainPanel.reset()
    }
}
