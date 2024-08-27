package com.vk.modulite.settings


import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

class ModuliteSettingsConfigurable : Configurable {
    data class Model(
        var turnOffIconsOnFolders: Boolean,
        var turnOffIconOnYaml: Boolean,
    )

    private val mainPanel: DialogPanel
    private val model = Model(
        turnOffIconsOnFolders = false,
        turnOffIconOnYaml = false,
    )

    init {
        mainPanel = panel {
            group("General") {
                row {
                    checkBox("Turn off modulite icon on folders").comment("Can improve indexing performance.")
                        .bindSelected(model::turnOffIconsOnFolders)
                }
                row {
                    checkBox("Turn off modulite icon on yaml file").comment("Can improve indexing performance.")
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

        val settings = ModuliteSettings.getInstance()
        return model.turnOffIconsOnFolders != settings.state.turnOffIconsOnFolders ||
                model.turnOffIconOnYaml != settings.state.turnOffIconOnYaml
    }

    override fun apply() {
        mainPanel.apply()

        val settings = ModuliteSettings.getInstance()
        with(settings) {
            state.turnOffIconsOnFolders = model.turnOffIconsOnFolders
            state.turnOffIconOnYaml = model.turnOffIconOnYaml
        }

    }

    override fun reset() {
        val settings = ModuliteSettings.getInstance()


        with(model) {
            turnOffIconsOnFolders = settings.state.turnOffIconsOnFolders
            turnOffIconOnYaml = settings.state.turnOffIconOnYaml
        }

        mainPanel.reset()
    }
}
