package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.model.MenuSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads the `gui` section of `config.yml` into [MenuSettings]. */
class MenuSettingsLoader(
    private val config: Config
) {
    fun load(): MenuSettings = MenuSettings(
        mainRows = config.getInt("gui.main.rows").coerceIn(1, MAX_ROWS),
        mainTitle = config.getString("gui.main.title"),
        storageDisplay = readStorageDisplay(),
        storageSlots = config.getInt("gui.main.storage-slots").coerceAtLeast(0),
        typeSelectRows = config.getInt("gui.type-select.rows").coerceIn(1, MAX_ROWS),
        typeSelectTitle = config.getString("gui.type-select.title")
    )

    private fun readStorageDisplay(): MenuSettings.StorageDisplay {
        val configured = config.getString("gui.main.storage-display").uppercase()
        return MenuSettings.StorageDisplay.entries.firstOrNull { it.name == configured }
            ?: MenuSettings.StorageDisplay.LORE
    }

    private companion object {
        const val MAX_ROWS = 6
    }
}
