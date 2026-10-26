package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.config.interfaces.Config

/** Layout of the minion menus, read from `config.yml`. */
data class MenuSettings(
    val mainRows: Int,
    val mainTitle: String,
    val storageDisplay: StorageDisplay,
    /** How many item types the storage area shows, highest count first. */
    val storageSlots: Int,
    val typeSelectRows: Int,
    val typeSelectTitle: String,
    /** The `gui.main` section, read for the main menu's optional layout keys. */
    val mainSection: Config,
    /** The `gui.type-select` section, read for the type select menu's optional layout keys. */
    val typeSelectSection: Config,
    /** If the main menu shows the stored XP button. */
    val showStoredXp: Boolean = true
) {
    /** Where a collected item's count is shown. */
    enum class StorageDisplay {
        TITLE,
        LORE,
        BOTH;

        val showsCountInName: Boolean
            get() = this == TITLE || this == BOTH

        val showsCountInLore: Boolean
            get() = this == LORE || this == BOTH
    }
}
