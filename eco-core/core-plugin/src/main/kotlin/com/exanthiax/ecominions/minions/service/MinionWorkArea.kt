package com.exanthiax.ecominions.minions.service

/**
 * The box a minion works in: its radius horizontally, its height stat
 * vertically, measured from one block below the minion upward.
 *
 * Shared so that everything which reaches into the box agrees on where it ends.
 * A self-seeder that seeds outside the box it mines places blocks it can never
 * reach again, which reads as a stuck minion.
 */
object MinionWorkArea {
    /** The box always starts one block below the minion, whatever the height. */
    const val FLOOR_OFFSET = -1

    /** Height counts the floor block, so a height of 3 reaches one block above the minion. */
    fun verticalOffsets(minion: Minion): IntRange =
        FLOOR_OFFSET..(FLOOR_OFFSET + minion.stats.height - 1)
}
