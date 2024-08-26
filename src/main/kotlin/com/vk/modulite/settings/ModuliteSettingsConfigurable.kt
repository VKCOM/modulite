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

        val settings = ModuliteSettingsState.getInstance()
        return model.turnOffIconsOnFolders != settings.turnOffIconsOnFolders ||
                model.turnOffIconOnYaml != settings.turnOffIconOnYaml
    }

    override fun apply() {
        mainPanel.apply()

        val settings = ModuliteSettingsState.getInstance()
        with(settings) {
            turnOffIconsOnFolders = model.turnOffIconsOnFolders
            turnOffIconOnYaml = model.turnOffIconOnYaml
        }

    }

    override fun reset() {
        val settings = ModuliteSettingsState.getInstance()


        with(model) {
            turnOffIconsOnFolders = settings.turnOffIconsOnFolders
            turnOffIconOnYaml = settings.turnOffIconOnYaml
        }

        mainPanel.reset()
    }
}
