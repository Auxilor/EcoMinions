package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionBlockIndex
import com.exanthiax.ecominions.minions.service.TreeService
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * The plant-wait-fell loop of a tree minion.
 *
 * A felled log is cleared rather than regenerated: the tree grows back from the
 * sapling this plants, so putting the log back would leave two trees' worth of
 * wood in the same place.
 */
class WoodcutterBehaviour(
    private val trees: TreeService,
    private val blocks: MinionBlockIndex
) : MinionBehaviour {

    override fun onBlockBroken(minion: Minion, block: Block, original: Material, now: Long) {
        block.type = Material.AIR
    }

    override fun onIdle(minion: Minion, now: Long): Boolean {
        if (!minion.state.type.placement.selfSeed) {
            return false
        }
        val definition = definitionFor(minion) ?: return false
        val plant = definition.plant ?: return false
        val planted = minion.mining.saplingPosition

        if (planted != null) {
            if (!plant.matches(planted.block)) {
                minion.mining.saplingPosition = null
            } else {
                if (now - minion.mining.saplingPlantedAt >= minion.stats.regenDelayTicks) {
                    fellTree(minion, definition, planted)
                }
                return true
            }
        }

        val spot = findPlantSpot(minion, definition) ?: return false
        plant.place(spot)
        minion.mining.saplingPosition = spot
        minion.mining.saplingPlantedAt = now
        return true
    }

    /**
     * The definition of the tree this minion fells.
     *
     * The first target that can actually grow - a woodcutter configured with a
     * log and nothing else has no sapling to plant, and stays blocked.
     */
    private fun definitionFor(minion: Minion): MinionBlock? =
        minion.state.type.targetBlocks
            .mapNotNull { blocks.forLookup(it.name) } // TODO(task-7): read TargetSet.definitions
            .firstOrNull { it.plant != null }

    private fun fellTree(minion: Minion, definition: MinionBlock, sapling: Location) {
        sapling.block.type = Material.AIR

        val leaves = trees.growTree(sapling, definition)
        minion.storage.store(trees.harvestLeaves(leaves))

        minion.mining.saplingPosition = null
    }

    /** The nearest air block on a valid surface with room for a tree above it. */
    private fun findPlantSpot(minion: Minion, definition: MinionBlock): Location? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val expectedSurface = minion.state.type.placement.surfaceBlock

        var best: Location? = null
        var bestDistance = Double.MAX_VALUE

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x == 0 && z == 0) {
                    continue
                }
                for (y in -1..1) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isPlantable(candidate, definition, expectedSurface)) {
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

    private fun isPlantable(block: Block, definition: MinionBlock, expectedSurface: Material?): Boolean {
        if (block.type != Material.AIR) {
            return false
        }
        val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
        val surfaceOk = when {
            definition.surface != null -> definition.surface.matches(below)
            expectedSurface != null -> below.type == expectedSurface
            else -> below.type.isSolid
        }
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
