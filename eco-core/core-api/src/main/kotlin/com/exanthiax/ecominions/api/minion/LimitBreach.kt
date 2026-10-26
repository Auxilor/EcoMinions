package com.exanthiax.ecominions.api.minion

/** Which limit a minion is counted against. */
enum class LimitScope {
    /** Every minion the owner has placed. */
    PLAYER,

    /** Every minion on the island the minion stands on. */
    ISLAND,

    /** The owner's minions of the same [MinionType]. */
    TYPE,

    /** The owner's minions with the same minion id. */
    MINION
}

/**
 * A limit a minion is over.
 *
 * @property count How many minions are placed in [scope], idle ones included.
 * @property max The cap for [scope].
 */
data class LimitBreach(val scope: LimitScope, val count: Int, val max: Int)
