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
    /** What self-seeding places. Null whenever [selfSeed] is false. */
    val seedBlock: TestableBlock? = null
) {
    companion object {
        val NONE = PlacementSettings(false, null)
    }
}
