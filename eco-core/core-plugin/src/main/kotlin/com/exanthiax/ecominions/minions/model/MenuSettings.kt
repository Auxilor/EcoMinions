package com.exanthiax.ecominions.minions.model

/** Layout of the minion menus, read from `config.yml`. */
data class MenuSettings(
    val mainRows: Int,
    val mainTitle: String,
    val storageDisplay: StorageDisplay,
    /** How many item types the storage area shows, highest count first. */
    val storageSlots: Int,
    val typeSelectRows: Int,
    val typeSelectTitle: String
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
