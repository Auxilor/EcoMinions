package com.exanthiax.ecominions.minions.service

import org.bukkit.block.Block

/**
 * The box a minion works in: its radius horizontally, its height stat
 * vertically, measured upward from its type's floor.
 *
 * Shared so that everything which reaches into the box agrees on where it ends.
 * A self-seeder that seeds outside the box it mines places blocks it can never
 * reach again, which reads as a stuck minion.
 */
object MinionWorkArea {
    fun floorOffset(minion: Minion): Int = minion.state.definition.baseStats.floorOffset

    /** Height counts the floor block, so a height of 3 from floor -1 reaches one block above the minion. */
    fun verticalOffsets(minion: Minion): IntRange {
        val floor = floorOffset(minion)
        return floor..(floor + minion.stats.height - 1)
    }

    /** Every block in the box except the minion's own column, which it stands in. */
    fun blocks(minion: Minion): List<Block> {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val heights = verticalOffsets(minion)
        return (-radius..radius).flatMap { x ->
            (-radius..radius).filterNot { z -> x == 0 && z == 0 }.flatMap { z ->
                heights.map { y -> world.getBlockAt(center.x + x, center.y + y, center.z + z) }
            }
        }
    }
}
