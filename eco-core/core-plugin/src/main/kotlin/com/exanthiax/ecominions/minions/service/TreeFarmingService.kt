package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.GameData
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * The plant-wait-fell loop of a tree minion, run when it has no log to mine.
 *
 * @return true from [tick] while the minion is busy, so it idles rather than
 * reporting that it is blocked.
 */
class TreeFarmingService(
    private val gameData: GameData,
    private val trees: TreeService,
    private val dropWarnings: DropWarningService
) {
    fun tick(minion: Minion, now: Long): Boolean {
        val family = minion.state.type.woodFamily ?: return false
        val planted = minion.mining.saplingPosition

        if (planted != null) {
            if (planted.block.type != family.sapling) {
                minion.mining.saplingPosition = null
            } else {
                if (now - minion.mining.saplingPlantedAt >= minion.stats.regenDelayTicks) {
                    fellTree(minion, planted)
                }
                return true
            }
        }

        val spot = findPlantSpot(minion) ?: return false
        spot.block.type = family.sapling
        minion.mining.saplingPosition = spot
        minion.mining.saplingPlantedAt = now
        return true
    }

    private fun fellTree(minion: Minion, sapling: Location) {
        val family = minion.state.type.woodFamily ?: return
        sapling.block.type = Material.AIR

        val leaves = trees.growTree(sapling, family)
        for (drop in trees.harvestLeaves(leaves)) {
            if (minion.state.type.priceOf(drop.type) == null) {
                dropWarnings.warnUnsellable(minion, drop.type)
                continue
            }
            if (minion.storage.addItem(drop).isNotEmpty()) {
                break
            }
        }

        minion.mining.saplingPosition = null
    }

    /** The nearest air block on a valid surface with room for a tree above it. */
    private fun findPlantSpot(minion: Minion): Location? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val expectedSurface = minion.state.type.placement.surfaceBlock
            ?: minion.state.type.woodFamily?.defaultSurface
            ?: gameData.surfaceForCrop(minion.state.type.targetBlocks.firstOrNull() ?: Material.AIR)

        var best: Location? = null
        var bestDistance = Double.MAX_VALUE

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x == 0 && z == 0) {
                    continue
                }
                for (y in -1..1) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isPlantable(candidate, expectedSurface)) {
                        continue
                    }
                    val distance = (x * x + y * y + z * z).toDouble()
                    if (distance < bestDistance) {
                        bestDistance = distance
                        best = candidate.location
                    }
                }
            }
        }
        return best
    }

    private fun isPlantable(block: Block, expectedSurface: Material?): Boolean {
        if (block.type != Material.AIR) {
            return false
        }
        val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
        val surfaceOk = if (expectedSurface != null) below.type == expectedSurface else below.type.isSolid
        return surfaceOk && hasClearance(block)
    }

    private fun hasClearance(block: Block): Boolean =
        (1..CLEARANCE_HEIGHT).all {
            block.world.getBlockAt(block.x, block.y + it, block.z).type == Material.AIR
        }

    private companion object {
        const val CLEARANCE_HEIGHT = 4
    }
}
