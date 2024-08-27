package com.vk.modulite.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
    name = "com.vk.modulite.settings.ModuliteSettings",
    storages = [Storage("ModulitePlugin.xml")]
)
class ModuliteSettings : PersistentStateComponent<ModuliteSettingsState> {
    companion object {
        fun getInstance() = service<ModuliteSettings>()
    }

    private var state = ModuliteSettingsState()

    override fun getState(): ModuliteSettingsState = state

    override fun loadState(state: ModuliteSettingsState) {
        this.state = state
    }
}
