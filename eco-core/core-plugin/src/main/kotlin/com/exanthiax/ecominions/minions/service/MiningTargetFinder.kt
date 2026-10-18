package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.TargetSet
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable

/**
 * Picks the next block a minion should break.
 *
 * A block already on the regen queue is never picked: its marker is a real
 * block of a material the minion may well target, and mining it would give the
 * minion free drops every cycle.
 *
 * The search box is [MinionWorkArea] - trees are the reason its height is
 * configurable, since their upper logs sit well above the three-block box that
 * used to be hardcoded here.
 */
class MiningTargetFinder(
    private val canWork: (Minion, Block) -> Boolean = { _, _ -> true }
) {
    fun find(minion: Minion): Block? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val targets = minion.state.definition.targetBlocks
        val candidates = mutableListOf<Block>()

        for (x in -radius..radius) {
            for (y in MinionWorkArea.verticalOffsets(minion)) {
                for (z in -radius..radius) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (minion.mining.isAwaitingRegen(candidate)) {
                        continue
                    }
                    if (targets.matches(candidate) && isMineableNow(candidate, targets) && canWork(minion, candidate)) {
                        candidates += candidate
                    }
                }
            }
        }
        return candidates.randomOrNull()
    }

    /** Crops must be ripe, and stacked plants are only cut at their base. */
    private fun isMineableNow(block: Block, targets: TargetSet): Boolean {
        val data = block.blockData
        if (data is Ageable && data.age < data.maximumAge) {
            return false
        }
        if (targets.definitionFor(block)?.stackedColumn == true) {
            val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
            if (below.type == block.type) {
                return false
            }
        }
        return true
    }
}
