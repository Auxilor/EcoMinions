package com.exanthiax.ecominions.minions.service

import org.bukkit.block.Block

/**
 * Keeps self-seeded plants `placement.spacing` blocks apart.
 *
 * Cactus breaks beside a solid block and trees grow their leaves into each
 * other, so some plants need room around them. The check covers every column
 * within [spacing] blocks, diagonals included, over the whole height of the
 * work area: a cactus column is a plant at every height it reaches, not only
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

        for (x in -spacing..spacing) {
            for (z in -spacing..spacing) {
                if (x == 0 && z == 0) {
                    continue
                }
                for (y in heights) {
                    if (isPlant(world.getBlockAt(candidate.x + x, baseY + y, candidate.z + z))) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
