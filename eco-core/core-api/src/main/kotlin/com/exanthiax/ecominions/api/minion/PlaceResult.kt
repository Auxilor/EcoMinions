package com.exanthiax.ecominions.api.minion

/** What an attempt to place a minion did. */
data class PlaceResult(
    val status: Status,
    /** The placed minion, or null unless [status] is [Status.PLACED]. */
    val minion: Minion?,
    /**
     * The limit that was hit, or 0 unless [status] is [Status.LIMIT_REACHED] or
     * [Status.ISLAND_LIMIT_REACHED]. For the first it is the player's limit, for
     * the second the island's.
     */
    val limit: Int
) {
    enum class Status {
        PLACED,
        WORLD_DISABLED,
        LIMIT_REACHED,

        /** The island the minion would stand on holds as many minions as it may. */
        ISLAND_LIMIT_REACHED,

        /** The player lacks the island privilege to place a minion there. */
        NO_ISLAND_PERMISSION,

        /** The player cannot build where the minion would stand. */
        PROTECTED,

        /** The item given to place from does not carry a minion. */
        INVALID_ITEM,
        CANCELLED
    }

    val isSuccess: Boolean
        get() = status == Status.PLACED
}
