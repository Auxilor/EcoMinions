package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.blocks.TestableBlock

/** How a mined block comes back. */
enum class RestoreMode {
    /** Put back exactly as it was, after the minion's regen delay. */
    REGEN,

    /** Placed again as a young plant, which then ripens in the world. */
    REPLANT,

    /** Felled: the block is cleared, and a sapling is planted to grow a new one. */
    TREE
}

/**
 * One `blocks/` definition: how a block is restored, and what is placed to restore it.
 *
 * A block with no definition regenerates, so servers only write a file for the
 * blocks whose behaviour they want to change - or for custom blocks, which this
 * plugin can know nothing about on its own.
 */
data class MinionBlock(
    val id: String,
    val lookup: String,
    val block: TestableBlock,
    val restore: RestoreMode,
    /** What self-seeding places. Null when nothing can be planted for this block. */
    val plant: TestableBlock?,
    /** The canopy of the grown tree. Required by [RestoreMode.TREE]. */
    val leaves: TestableBlock?,
    /** What must be underneath to plant. Null means any solid block will do. */
    val surface: TestableBlock?,
    /** Overrides which shape in `trees.json` this block grows as. */
    val treePattern: String?,
    /** Item ids credited during offline catch-up, for blocks whose drops cannot be read. */
    val drops: List<String>
) {
    val isPlantable: Boolean
        get() = plant != null

    /** The `trees.json` key to grow this block with. */
    val patternKey: String
        get() = treePattern ?: id

    companion object {
        /** What an undefined block does: come back as it was. */
        fun regenDefault(id: String, lookup: String, block: TestableBlock) = MinionBlock(
            id = id,
            lookup = lookup,
            block = block,
            restore = RestoreMode.REGEN,
            plant = null,
            leaves = null,
            surface = null,
            treePattern = null,
            drops = emptyList()
        )
    }
}
