package com.exanthiax.ecominions.upgrades.model

/** Layout of the upgrade menus, read from `config.yml`. */
data class UpgradeMenuSettings(
    val listRows: Int,
    val listTitle: String,
    val upgradeRows: Int,
    val upgradeTitle: String,
    val costSelectRows: Int,
    val costSelectTitle: String
)
