package com.vk.modulite.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
    name = "com.vk.modulite.settings.ModuliteSettingsState",
    storages = [Storage("ModulitePlugin.xml")]
)
class ModuliteSettingsState : PersistentStateComponent<ModuliteSettingsState?> {
    companion object {
        fun getInstance() = service<ModuliteSettingsState>()
    }

    var turnOffIconsOnFolders = false
    var turnOffIconOnYaml = false

    override fun getState() = this

    override fun loadState(state: ModuliteSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
