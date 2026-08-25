package com.exanthiax.ecominions.minions.service

import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Places a minion's own block to mine when there is nothing left in range.
 *
 * Only minions that opt in via `placement.self-seed` seed themselves; the rest
 * stay blocked until a player supplies blocks. Woodcutters are seeded by
 * [com.exanthiax.ecominions.minions.service.behaviour.WoodcutterBehaviour]
 * instead, which has a plant-wait-fell cycle of its own.
 */
class SeedingService {
    /** @return true if a block was seeded, so the minion idles rather than reporting blocked. */
    fun tick(minion: Minion): Boolean {
        val placement = minion.state.type.placement
        if (!placement.selfSeed) {
            return false
        }
        val seed = placement.seedBlock ?: return false
        // TODO(task-7): fall back to the seeded block's definition surface
        val surface = placement.surfaceBlock

        val spot = findSpot(minion, surface) ?: return false
        spot.type = seed
        return true
    }

    /** The nearest air block on a valid surface, never the minion's own column. */
    private fun findSpot(minion: Minion, expectedSurface: Material?): Block? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world

        var best: Block? = null
        var bestDistance = Int.MAX_VALUE

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x == 0 && z == 0) {
                    continue
                }
                for (y in -1..1) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isSeedable(candidate, expectedSurface)) {
                        continue
                    }
                    val distance = x * x + y * y + z * z
                    if (distance < bestDistance) {
                        bestDistance = distance
                        best = candidate
                    }
                }
            }
        }
        return best
    }

    private fun isSeedable(block: Block, expectedSurface: Material?): Boolean {
        if (block.type != Material.AIR) {
            return false
        }
        val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
        return if (expectedSurface != null) below.type == expectedSurface else below.isSolid
    }
}
