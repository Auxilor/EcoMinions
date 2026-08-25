package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.data.JsonData
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.google.gson.JsonObject
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/** Grows and harvests the trees that tree-type minions farm. */
class TreeService {

    private val patterns: Patterns by lazy { loadPatterns() }

    /**
     * Replaces the sapling at [base] with a full tree.
     *
     * @return The leaf blocks placed, for the caller to harvest. A definition
     * with no [MinionBlock.leaves] grows a bare trunk rather than nothing, so a
     * half-configured tree still farms wood.
     */
    fun growTree(base: Location, definition: MinionBlock): List<Block> {
        val world = base.world ?: return emptyList()
        val pattern = patterns.byFamily[definition.patternKey] ?: patterns.default
        val leaves = mutableListOf<Block>()

        for (placement in pattern.blocks) {
            val block = world.getBlockAt(
                base.blockX + placement.x,
                base.blockY + placement.y,
                base.blockZ + placement.z
            )
            when (placement.kind) {
                "log" -> if (block.type == Material.AIR || definition.block.matches(block)) {
                    definition.block.place(block.location)
                }

                "leaf" -> if (block.type == Material.AIR) {
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

    private fun loadPatterns(): Patterns {
        val root = JsonData.read("/data/trees.json")
        val byFamily = root.getAsJsonObject("by-family")
            ?.entrySet()
            ?.associate { (id, value) -> id to readPattern(value.asJsonObject) }
            .orEmpty()
        return Patterns(readPattern(root.getAsJsonObject("default")), byFamily)
    }

    private fun readPattern(obj: JsonObject): Pattern =
        Pattern(
            obj.getAsJsonArray("blocks").map { element ->
                val block = element.asJsonObject
                PatternBlock(
                    block["x"].asInt,
                    block["y"].asInt,
                    block["z"].asInt,
                    block["kind"].asString
                )
            }
        )

    private class Patterns(val default: Pattern, val byFamily: Map<String, Pattern>)

    private class Pattern(val blocks: List<PatternBlock>)

    private class PatternBlock(val x: Int, val y: Int, val z: Int, val kind: String)
}
