package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.data.JsonData
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.TreeShape
import com.google.gson.JsonObject
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/** Grows and harvests the trees that woodcutter minions farm. */
class TreeService {

    private val shapes: Shapes by lazy { loadShapes() }

    /**
     * Replaces the sapling at [base] with a full tree.
     *
     * @return The leaf blocks placed, for the caller to harvest. A definition
     * with no [MinionBlock.leaves] grows a bare trunk rather than nothing, so a
     * half-configured tree still farms wood.
     */
    fun growTree(base: Location, definition: MinionBlock): List<Block> {
        val world = base.world ?: return emptyList()
        val shape = shapes.bySpecies[definition.patternKey] ?: shapes.default
        val leaves = mutableListOf<Block>()

        for (placement in shape.blocks()) {
            val block = world.getBlockAt(
                base.blockX + placement.x,
                base.blockY + placement.y,
                base.blockZ + placement.z
            )
            when (placement.kind) {
                TreeBlockKind.LOG -> if (block.type == Material.AIR || definition.block.matches(block)) {
                    definition.block.place(block.location)
                }

                TreeBlockKind.LEAF -> if (block.type == Material.AIR) {
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

    private fun loadShapes(): Shapes {
        val root = JsonData.read("/data/trees.json")
        val bySpecies = root.getAsJsonObject("by-species")
            ?.entrySet()
            ?.associate { (id, value) -> id to readShape(value.asJsonObject) }
            .orEmpty()
        return Shapes(readShape(root.getAsJsonObject("default")), bySpecies)
    }

    /**
     * An unreadable field falls back to the generic shape's value rather than
     * failing the load: these files ship with the plugin, so a bad entry means
     * a hand edit, and growing the wrong shape beats growing nothing.
     */
    private fun readShape(obj: JsonObject): TreeShape {
        val fallback = TreeShape.DEFAULT
        return TreeShape(
            trunk = obj["trunk"]?.asInt?.coerceAtLeast(1) ?: fallback.trunk,
            radius = obj["radius"]?.asInt?.coerceAtLeast(0) ?: fallback.radius,
            layers = obj["layers"]?.asInt?.coerceAtLeast(0) ?: fallback.layers,
            style = obj["style"]?.asString?.let { style ->
                TreeShape.Style.entries.firstOrNull { it.name.equals(style, ignoreCase = true) }
            } ?: fallback.style
        )
    }

    private class Shapes(val default: TreeShape, val bySpecies: Map<String, TreeShape>)
}

private typealias TreeBlockKind = com.exanthiax.ecominions.minions.model.TreeBlock.Kind
