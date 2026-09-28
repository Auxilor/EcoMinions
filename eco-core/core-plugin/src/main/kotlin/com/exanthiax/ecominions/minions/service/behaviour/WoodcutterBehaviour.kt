package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.TargetSet
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.SeedSurface
import com.exanthiax.ecominions.minions.service.TreeService
import com.willfp.eco.core.blocks.TestableBlock
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
    private val trees: TreeService
) : MinionBehaviour {

    override fun onBlockBroken(
        minion: Minion,
        block: Block,
        definition: MinionBlock?,
        original: Material,
        now: Long
    ) {
        block.type = Material.AIR
        harvestIfFelled(minion)
    }

    /**
     * Takes the leaves once the last log of the standing tree is gone.
     *
     * Leaves are not a target block, so nothing else would ever clear them, and
     * a tree left leafless while its trunk is still standing looks wrong. This
     * is also where the drops the leaves owe the minion are collected.
     */
    private fun harvestIfFelled(minion: Minion) {
        val tree = minion.mining.standingTree ?: return
        if (tree.logs.any { it.type != Material.AIR }) {
            return
        }
        minion.storage.store(trees.harvestLeaves(tree.leaves))
        minion.mining.standingTree = null
    }

    override fun onIdle(minion: Minion, now: Long): Boolean {
        if (!minion.state.definition.placement.selfSeed) {
            return false
        }
        // A player can clear the trunk themselves, which never reaches
        // onBlockBroken - the leaves would then block the next sapling.
        harvestIfFelled(minion)
        val definition = definitionFor(minion) ?: return false
        val plant = definition.plant ?: return false
        val planted = minion.mining.saplingPosition

        if (planted != null) {
            if (!plant.matches(planted.block)) {
                minion.mining.saplingPosition = null
            } else {
                if (now - minion.mining.saplingPlantedAt >= minion.stats.regenDelayTicks) {
                    growTree(minion, definition, planted)
                }
                return true
            }
        }

        val spot = findPlantSpot(minion, definition) ?: return false
        SeedSurface.prepare(SeedSurface.below(spot),surfaceFor(minion, definition))
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
        minion.state.definition.targetBlocks.definitions.firstOrNull { it.plant != null }

    /**
     * Turns a matured sapling into a standing tree.
     *
     * The tree is left whole: its logs become the minion's targets and its
     * leaves stay up until [harvestIfFelled] takes them with the last log.
     */
    private fun growTree(minion: Minion, definition: MinionBlock, sapling: Location) {
        sapling.block.type = Material.AIR

        minion.mining.standingTree = trees.growTree(sapling, definition)
        minion.mining.saplingPosition = null
    }

    /** The nearest air block on a valid surface with room for a tree above it. */
    private fun findPlantSpot(minion: Minion, definition: MinionBlock): Location? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val expectedSurface = surfaceFor(minion, definition)
        val targets = minion.state.definition.targetBlocks

        var best: Location? = null
        var bestDistance = Double.MAX_VALUE

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x == 0 && z == 0) {
                    continue
                }
                for (y in -1..1) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isPlantable(candidate, expectedSurface, targets)) {
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

    /** The tree's own surface wins over the minion's. */
    private fun surfaceFor(minion: Minion, definition: MinionBlock): TestableBlock? =
        definition.surface ?: minion.state.definition.placement.surfaceBlock

    private fun isPlantable(block: Block, expectedSurface: TestableBlock?, targets: TargetSet): Boolean {
        if (block.type != Material.AIR) {
            return false
        }
        return SeedSurface.accepts(SeedSurface.below(block), expectedSurface, targets) &&
            hasClearance(block)
    }

    private fun hasClearance(block: Block): Boolean =
        (1..CLEARANCE_HEIGHT).all {
            block.world.getBlockAt(block.x, block.y + it, block.z).type == Material.AIR
        }

    private companion object {
        const val CLEARANCE_HEIGHT = 4
    }
}
