package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.TreeBlock
import com.exanthiax.ecominions.minions.model.TreeShape
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
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
     * @return The leaf blocks placed, for the caller to harvest. A definition
     * with no [MinionBlock.leaves] grows a bare trunk rather than nothing, so a
     * half-configured tree still farms wood.
     */
    fun growTree(base: Location, definition: MinionBlock): List<Block> {
        val world = base.world ?: return emptyList()
        val shape = TreeShape.forSpecies(definition.patternKey) ?: TreeShape.DEFAULT
        val leaves = mutableListOf<Block>()

        for (placement in shape.blocks()) {
            val block = world.getBlockAt(
                base.blockX + placement.x,
                base.blockY + placement.y,
                base.blockZ + placement.z
            )
            when (placement.kind) {
                TreeBlock.Kind.LOG -> if (block.type == Material.AIR || definition.block.matches(block)) {
                    definition.block.place(block.location)
                }

                TreeBlock.Kind.LEAF -> if (block.type == Material.AIR) {
                    definition.leaves?.let {
                        it.place(block.location)
                        leaves += block
                    }
                }
            }
        }

        return leaves
    }

    /** Breaks [leaves] and returns their drops. */
    fun harvestLeaves(leaves: List<Block>): List<ItemStack> {
        val harvested = mutableListOf<ItemStack>()
        for (leaf in leaves) {
            harvested += leaf.getDrops()
            leaf.type = Material.AIR
        }
        return harvested
    }
}
