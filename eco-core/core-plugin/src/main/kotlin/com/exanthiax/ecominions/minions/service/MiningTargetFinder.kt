package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.GameData
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable

/**
 * Picks the next block a minion should break.
 *
 * The search box is the minion's radius horizontally and its height stat
 * vertically, measured from one block below the minion upward - trees are the
 * reason height is configurable, since their upper logs sit well above the
 * three-block box that used to be hardcoded here.
 */
class MiningTargetFinder(
    private val gameData: GameData
) {
    fun find(minion: Minion): Block? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val targets = minion.state.type.targetBlocks
        val candidates = mutableListOf<Block>()

        for (x in -radius..radius) {
            for (y in FLOOR_OFFSET..topOffset(minion)) {
                for (z in -radius..radius) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (targets.matches(candidate) && isMineableNow(candidate)) {
                        candidates += candidate
                    }
                }
            }
        }
        return candidates.randomOrNull()
    }

    /** Height counts the floor block, so a height of 3 reaches one block above the minion. */
    private fun topOffset(minion: Minion): Int = FLOOR_OFFSET + minion.stats.height - 1

    /** Crops must be ripe, and stacked plants are only cut at their base. */
    private fun isMineableNow(block: Block): Boolean {
        val data = block.blockData
        if (data is Ageable && data.age < data.maximumAge) {
            return false
        }
        if (gameData.growthRules.isStackedColumn(block.type)) {
            val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
            if (below.type == block.type) {
                return false
            }
        }
        return true
    }

    private companion object {
        /** The box always starts one block below the minion, whatever the height. */
        const val FLOOR_OFFSET = -1
    }
}
