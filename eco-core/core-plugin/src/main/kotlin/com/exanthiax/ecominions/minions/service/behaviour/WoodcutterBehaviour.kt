package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.TargetSet
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.PlantSpacing
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
        harvestFelled(minion)
    }

    /**
     * Grows every sapling that has matured, then plants one more if there is a
     * spaced spot left, so the area fills with trees before the first is felled.
     *
     * @return true if a sapling was planted, which spends the cycle.
     */
    override fun onBeforeMine(minion: Minion, now: Long): Boolean {
        if (!minion.state.definition.placement.selfSeed) {
            return false
        }
        // A player can clear a trunk themselves, which never reaches
        // onBlockBroken - its leaves would then block the next sapling.
        harvestFelled(minion)
        val definition = definitionFor(minion) ?: return false
        val plant = definition.plant ?: return false

        growMatured(minion, definition, plant, now)

        val spot = findPlantSpot(minion, definition) ?: return false
        SeedSurface.prepare(SeedSurface.below(spot), surfaceFor(minion, definition))
        plant.place(spot)
        minion.mining.saplings[spot] = now
        minion.mining.seeded += spot.block.location
        return true
    }

    /** Nothing to fell, but a sapling still growing is waiting rather than blocked. */
    override fun onIdle(minion: Minion, now: Long): Boolean = minion.mining.saplings.isNotEmpty()

    override fun canStock(minion: Minion): Boolean {
        if (!minion.state.definition.placement.selfSeed) {
            return false
        }
        val definition = definitionFor(minion) ?: return false
        return minion.mining.saplings.isNotEmpty() ||
            minion.mining.standingTrees.isNotEmpty() ||
            findPlantSpot(minion, definition) != null
    }

    /**
     * Takes the leaves of every tree whose last log is gone.
     *
     * Leaves are not a target block, so nothing else would ever clear them, and
     * a tree left leafless while its trunk is still standing looks wrong. This
     * is also where the drops the leaves owe the minion are collected.
     */
    private fun harvestFelled(minion: Minion) {
        val felled = minion.mining.standingTrees.filter { tree -> tree.logs.all { it.type == Material.AIR } }
        for (tree in felled) {
            minion.storage.store(trees.harvestLeaves(tree.leaves))
            minion.mining.standingTrees.remove(tree)
        }
    }

    /** Turns each sapling that has waited out the regen delay into a standing tree. */
    private fun growMatured(minion: Minion, definition: MinionBlock, plant: TestableBlock, now: Long) {
        val saplings = minion.mining.saplings.entries.iterator()
        while (saplings.hasNext()) {
            val (sapling, plantedAt) = saplings.next()
            if (!plant.matches(sapling.block)) {
                saplings.remove()
                continue
            }
            if (now - plantedAt < minion.stats.regenDelayTicks) {
                continue
            }
            sapling.block.type = Material.AIR
            val grown = trees.growTree(sapling, definition, minion.entity.location)
            minion.mining.standingTrees += grown
            (grown.logs + grown.leaves).mapTo(minion.mining.seeded) { it.location }
            saplings.remove()
        }
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
     * The nearest air block on a valid surface with room for a tree above it,
     * kept as far from the minion as from any other tree.
     */
    private fun findPlantSpot(minion: Minion, definition: MinionBlock): Location? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val expectedSurface = surfaceFor(minion, definition)
        val targets = minion.state.definition.targetBlocks
        val spacing = minion.state.definition.placement.spacing
        val plant = definition.plant

        var best: Location? = null
        var bestDistance = Double.MAX_VALUE

        val minionGap = spacing.coerceAtLeast(MIN_MINION_GAP)

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x in -minionGap..minionGap && z in -minionGap..minionGap) {
                    continue
                }
                for (y in -1..1) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isPlantable(candidate, expectedSurface, targets)) {
                        continue
                    }
                    val clear = PlantSpacing.isClear(minion, candidate, spacing) {
                        targets.matches(it) || plant?.matches(it) == true
                    }
                    if (!clear) {
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

        /** A trunk is never planted right beside the minion, whatever the spacing. */
        const val MIN_MINION_GAP = 1
    }
}
