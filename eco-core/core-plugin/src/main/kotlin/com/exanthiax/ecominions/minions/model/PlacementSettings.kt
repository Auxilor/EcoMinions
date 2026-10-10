package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.blocks.TestableBlock

/**
 * Whether mined blocks regenerate one block higher than they were broken, what
 * surface they need beneath them when they do, and whether the minion supplies
 * its own blocks to mine.
 *
 * A minion that does not self-seed only ever mines blocks a player put there.
 */
data class PlacementSettings(
    val liftRegen: Boolean,
    val surfaceBlock: TestableBlock?,
    val selfSeed: Boolean = false,
    /** What self-seeding places, each in turn. Empty whenever [selfSeed] is false. */
    val seedBlocks: List<TestableBlock> = emptyList(),
    /**
     * Empty blocks self-seeding keeps between each planted column, along X and
     * Z only - diagonals are not counted. 0 packs them side by side.
     */
    val spacing: Int = 0
) {
    companion object {
        val NONE = PlacementSettings(false, null)
    }
}
