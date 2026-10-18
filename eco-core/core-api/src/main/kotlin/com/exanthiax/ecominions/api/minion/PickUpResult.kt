package com.exanthiax.ecominions.api.minion

import org.bukkit.inventory.ItemStack

/** What an attempt to pick a minion up did. */
data class PickUpResult(
    val status: Status,
    /** The item given to the player, or null unless [status] is [Status.PICKED_UP]. */
    val item: ItemStack?
) {
    enum class Status {
        PICKED_UP,
        NOT_OWNER,

        /** The player lacks the island privilege to pick the minion up. */
        NO_ISLAND_PERMISSION,
        CANCELLED
    }

    val isSuccess: Boolean
        get() = status == Status.PICKED_UP
}
