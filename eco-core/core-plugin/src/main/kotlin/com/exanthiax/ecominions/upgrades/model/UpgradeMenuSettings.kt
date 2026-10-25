package com.exanthiax.ecominions.upgrades.model

import com.willfp.eco.core.config.interfaces.Config

/** Layout of the upgrade menus, read from `config.yml`. */
data class UpgradeMenuSettings(
    val listRows: Int,
    val listTitle: String,
    val upgradeRows: Int,
    val upgradeTitle: String,
    val costSelectRows: Int,
    val costSelectTitle: String,
    /** The `gui.upgrade-list` section, read for the upgrade list menu's optional layout keys. */
    val upgradeListSection: Config,
    /** The `gui.upgrade` section, read for the upgrade menu's optional layout keys. */
    val upgradeSection: Config,
    /** The `gui.cost-select` section, read for the cost select menu's optional layout keys. */
    val costSelectSection: Config
)
