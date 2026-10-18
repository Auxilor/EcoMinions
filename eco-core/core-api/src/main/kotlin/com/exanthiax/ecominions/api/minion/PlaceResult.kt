package com.exanthiax.ecominions.api.minion

/** What an attempt to place a minion did. */
data class PlaceResult(
    val status: Status,
    /** The placed minion, or null unless [status] is [Status.PLACED]. */
    val minion: Minion?,
    /** The player's placement limit, or 0 unless [status] is [Status.LIMIT_REACHED]. */
    val limit: Int
) {
    enum class Status {
        PLACED,
        WORLD_DISABLED,
        LIMIT_REACHED,

        /** The player cannot build where the minion would stand. */
        PROTECTED,

        /** The item given to place from does not carry a minion. */
        INVALID_ITEM,
        CANCELLED
    }

    val isSuccess: Boolean
        get() = status == Status.PLACED
}
