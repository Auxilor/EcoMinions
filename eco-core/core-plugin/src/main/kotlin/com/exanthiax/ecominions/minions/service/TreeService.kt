package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.TreeBlock
import com.exanthiax.ecominions.minions.model.TreeShape
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Leaves
import org.bukkit.inventory.ItemStack

/** Grows and harvests the trees that woodcutter minions farm. */
class TreeService {

    /**
     * Replaces the sapling at [base] with a full tree.
     *
     * The shape comes from the definition's [MinionBlock.patternKey], so a
     * custom log named after no known species grows [TreeShape.DEFAULT] unless
     * its definition picks one with `tree-pattern`.
     *
     * Leaves are never placed in the column at [keepClear], so a canopy cannot
     * grow through the minion standing there, and nothing is placed where
     * [canPlace] refuses.
     *
     * @return The tree that now stands there. A definition with no
     * [MinionBlock.leaves] grows a bare trunk rather than nothing, so a
     * half-configured tree still farms wood.
     */
    fun growTree(
        base: Location,
        definition: MinionBlock,
        keepClear: Location? = null,
        canPlace: (Block) -> Boolean = { true }
    ): GrownTree {
        val world = base.world ?: return GrownTree.NONE
        val shape = TreeShape.forSpecies(definition.patternKey) ?: TreeShape.DEFAULT
        val logs = mutableListOf<Block>()
        val leaves = mutableListOf<Block>()

        for (placement in shape.blocks()) {
            val block = world.getBlockAt(
                base.blockX + placement.x,
                base.blockY + placement.y,
                base.blockZ + placement.z
            )
            if (!canPlace(block)) {
                continue
            }
            when (placement.kind) {
                TreeBlock.Kind.LOG -> if (block.type == Material.AIR || definition.block.matches(block)) {
                    definition.block.place(block.location)
                    logs += block
                }

                TreeBlock.Kind.LEAF -> if (block.type == Material.AIR && !block.isInColumnOf(keepClear)) {
                    definition.leaves?.let {
                        keepFromDecaying(it.place(block.location))
                        leaves += block
                    }
                }
            }
        }

        return GrownTree(logs, leaves)
    }

    /**
     * Placed leaves count as natural and decay once the logs holding them up are
     * felled, which drops saplings and sticks on the ground before the minion
     * collects the canopy.
     */
    private fun keepFromDecaying(block: Block) {
        val data = block.blockData as? Leaves ?: return
        data.isPersistent = true
        block.blockData = data
    }

    private fun Block.isInColumnOf(location: Location?): Boolean =
        location != null && x == location.blockX && z == location.blockZ

    /**
     * Breaks [leaves] and returns their drops.
     *
     * Leaves that a player has already cleared are skipped rather than dropping
     * a second time.
     */
    fun harvestLeaves(leaves: List<Block>): List<ItemStack> {
        val harvested = mutableListOf<ItemStack>()
        for (leaf in leaves) {
            if (leaf.type == Material.AIR) {
                continue
            }
            harvested += leaf.getDrops()
            leaf.type = Material.AIR
        }
        return harvested
    }

    /**
     * A tree standing in the world, as it was placed.
     *
     * The logs are what the minion fells one at a time; the leaves stay up until
     * the last of those logs is gone, so a grown tree looks like a tree for the
     * whole time it takes to cut down.
     */
    class GrownTree(
        val logs: List<Block>,
        val leaves: List<Block>
    ) {
        companion object {
            val NONE = GrownTree(emptyList(), emptyList())
        }
    }
}
