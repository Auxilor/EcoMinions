package com.exanthiax.ecominions.minions.service

import com.willfp.eco.core.blocks.TestableBlock
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Stocks a minion's work area with the blocks it mines, one block at a time.
 *
 * Only minions that opt in via `placement.self-seed` seed themselves; the rest
 * stay blocked until a player supplies blocks. Woodcutters are seeded by
 * [com.exanthiax.ecominions.minions.service.behaviour.WoodcutterBehaviour]
 * instead, which has a plant-wait-fell cycle of its own.
 */
class SeedingService {
    /**
     * Seeds a single block, if there is anywhere left to seed.
     *
     * Runs before mining and takes the whole cycle, so the area is filled
     * before the first block is broken: pacing seeding on its own timer let the
     * minion mine whatever it had already placed whenever a seed was not yet
     * due, and the area never filled.
     *
     * @return true if a block was seeded, so the caller spends the cycle on
     * seeding rather than mining what was just placed.
     */
    fun tick(minion: Minion): Boolean {
        val spot = pendingSpot(minion) ?: return false
        minion.state.definition.placement.seedBlock?.place(spot.location) ?: return false
        return true
    }

    /**
     * @return true if there is somewhere left to seed, whether or not it is due.
     *
     * A minion with nothing to mine and a seed on the way is waiting, not
     * blocked, and should not be reported as if a player had to feed it.
     */
    fun isPending(minion: Minion): Boolean = pendingSpot(minion) != null

    private fun pendingSpot(minion: Minion): Block? {
        val placement = minion.state.definition.placement
        if (!placement.selfSeed || placement.seedBlock == null) {
            return null
        }
        return findSpot(minion, placement.surfaceBlock)
    }

    /**
     * The nearest air block on a valid surface, never the minion's own column.
     *
     * The search is [MinionWorkArea], the same box the minion mines in: seeding
     * above the box it can reach leaves ore stranded above the minion forever.
     */
    private fun findSpot(minion: Minion, expectedSurface: TestableBlock?): Block? {
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
                for (y in MinionWorkArea.verticalOffsets(minion)) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isSeedable(minion, candidate, expectedSurface)) {
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

    private fun isSeedable(minion: Minion, block: Block, expectedSurface: TestableBlock?): Boolean {
        if (block.type != Material.AIR) {
            return false
        }
        if (minion.mining.isAwaitingRegen(block)) {
            return false
        }
        val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
        return if (expectedSurface != null) expectedSurface.matches(below) else below.isSolid
    }
}
