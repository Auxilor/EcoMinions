package com.exanthiax.ecominions.minions.service

import org.bukkit.block.Block

/**
 * Keeps self-seeded plants `placement.spacing` blocks apart.
 *
 * Cactus breaks beside a solid block and trees grow their leaves into each
 * other, so some plants need room around them. The check covers the columns
 * up to [spacing] blocks away along X and Z - diagonals are not counted -
 * over the whole height of the work area: a cactus column is a plant at every height it reaches, not only
 * at the layer being seeded.
 *
 * The candidate's own column is skipped - what sits beneath a seed is its
 * surface, not a neighbour.
 */
object PlantSpacing {
    fun isClear(minion: Minion, candidate: Block, spacing: Int, isPlant: (Block) -> Boolean): Boolean {
        if (spacing <= 0) {
            return true
        }
        val world = candidate.world
        val baseY = minion.entity.location.block.y
        val heights = MinionWorkArea.verticalOffsets(minion)

        for (distance in 1..spacing) {
            for ((dx, dz) in DIRECTIONS) {
                for (y in heights) {
                    val block = world.getBlockAt(candidate.x + dx * distance, baseY + y, candidate.z + dz * distance)
                    if (isPlant(block)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private val DIRECTIONS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
}
