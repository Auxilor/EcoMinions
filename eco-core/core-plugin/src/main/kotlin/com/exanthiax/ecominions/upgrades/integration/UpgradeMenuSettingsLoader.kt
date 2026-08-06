package com.exanthiax.ecominions.upgrades.integration

import com.exanthiax.ecominions.upgrades.model.UpgradeMenuSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads the upgrade menu layout out of `config.yml`. */
class UpgradeMenuSettingsLoader(
    private val config: Config
) {
    fun load(): UpgradeMenuSettings = UpgradeMenuSettings(
        listRows = config.getInt("gui.upgrade-list.rows").coerceIn(1, MAX_ROWS),
        listTitle = config.getString("gui.upgrade-list.title"),
        upgradeRows = config.getInt("gui.upgrade.rows").coerceIn(1, MAX_ROWS),
        upgradeTitle = config.getString("gui.upgrade.title"),
        costSelectRows = config.getInt("gui.cost-select.rows").coerceIn(1, MAX_ROWS),
        costSelectTitle = config.getString("gui.cost-select.title")
    )

    private companion object {
        const val MAX_ROWS = 6
    }
}
