package com.exanthiax.ecominions.minions.model

/** One block a grown tree places, relative to its sapling. */
data class TreeBlock(val x: Int, val y: Int, val z: Int, val kind: Kind) {
    enum class Kind { LOG, LEAF }
}

/**
 * The shape a tree grows in, as parameters rather than a list of coordinates.
 *
 * Authoring nine species as explicit block lists is several hundred numbers
 * nobody can check by eye, which is how `trees.json` ended up shipping an empty
 * `by-family` and growing every species as the same generic stub.
 *
 * ### Trunk height and the minion's reach
 *
 * A woodcutter fells a tree by mining its logs, so every log has to sit inside
 * the minion's search box. That box runs from one block below the minion up to
 * `height - 2`, and a sapling is planted up to one block above the minion, so
 * with the default `height: 6` the highest reachable log is [MAX_REACHABLE_TRUNK]
 * blocks above the sapling. A taller trunk needs a taller `height` on the minion
 * or its top logs are left standing.
 */
data class TreeShape(
    /** Logs placed straight up from the sapling. */
    val trunk: Int,
    /** How far the canopy reaches horizontally from the trunk. */
    val radius: Int,
    /** How many levels of the trunk the canopy wraps. */
    val layers: Int,
    val style: Style
) {
    enum class Style {
        /** A rounded blob around the top of the trunk. Oak, birch, cherry. */
        ROUND,

        /** Rings that narrow with height, like a spruce. */
        CONIFER,

        /** One wide slab at the top of the trunk, like an acacia. */
        FLAT
    }

    /**
     * @return Every block of the grown tree, relative to the sapling.
     *
     * Ordered trunk-first so a caller placing them in sequence never drops a
     * leaf onto a space a log is about to take.
     */
    fun blocks(): List<TreeBlock> = trunk() + canopy() + cap()

    private fun trunk(): List<TreeBlock> =
        (0 until trunk).map { TreeBlock(0, it, 0, TreeBlock.Kind.LOG) }

    private fun canopy(): List<TreeBlock> = when (style) {
        Style.ROUND -> round()
        Style.CONIFER -> conifer()
        Style.FLAT -> slab(trunk - 1, radius)
    }

    private fun round(): List<TreeBlock> =
        (0 until layers).flatMap { layer ->
            val y = trunk - 1 - layer
            // The topmost layer loses its corners, which is what stops a
            // rounded canopy reading as a cube.
            slab(y, radius, trimCorners = layer == 0)
        }

    private fun conifer(): List<TreeBlock> =
        (0 until layers).flatMap { layer ->
            val y = trunk - 1 - layer
            slab(y, (radius - (layers - 1 - layer)).coerceAtLeast(1))
        }

    /** One flat level of leaves, minus the trunk column running through it. */
    private fun slab(y: Int, radius: Int, trimCorners: Boolean = false): List<TreeBlock> =
        (-radius..radius).flatMap { x ->
            (-radius..radius).mapNotNull { z ->
                when {
                    x == 0 && z == 0 -> null
                    // A one-block canopy trimmed of its corners is just a plus,
                    // which reads as a shrub rather than a small tree.
                    trimCorners && radius >= 2 &&
                        kotlin.math.abs(x) == radius && kotlin.math.abs(z) == radius -> null
                    else -> TreeBlock(x, y, z, TreeBlock.Kind.LEAF)
                }
            }
        }

    /** The crown sitting on top of the trunk: a single leaf, or a plus for a wider tree. */
    private fun cap(): List<TreeBlock> {
        val reach = if (radius <= 1) 0 else 1
        return (-reach..reach).flatMap { x ->
            (-reach..reach).mapNotNull { z ->
                TreeBlock(x, trunk, z, TreeBlock.Kind.LEAF)
                    .takeIf { kotlin.math.abs(x) + kotlin.math.abs(z) <= reach }
            }
        }
    }

    companion object {
        /**
         * The tallest trunk a minion on the default `height: 6` can fell.
         *
         * The search box tops out at `height - 2` above the minion and a sapling
         * may be planted one block above it, so a trunk of five would leave its
         * top log unreachable.
         */
        const val MAX_REACHABLE_TRUNK = 4

        val DEFAULT = TreeShape(trunk = MAX_REACHABLE_TRUNK, radius = 1, layers = 1, style = Style.ROUND)
    }
}
