package com.exanthiax.ecominions.minions.service

import org.bukkit.entity.Entity
import kotlin.math.abs

/** Finds entities inside a minion's work box. A seam, so activities can be tested without a world. */
interface WorkAreaEntities {
    fun <T : Entity> find(minion: Minion, type: Class<T>): List<T>
}

class BukkitWorkAreaEntities : WorkAreaEntities {
    override fun <T : Entity> find(minion: Minion, type: Class<T>): List<T> {
        val base = minion.entity.location.block
        val radius = minion.stats.radius
        val heights = MinionWorkArea.verticalOffsets(minion)
        val halfHeight = (heights.last - heights.first + 1) / 2.0
        val center = base.location.add(0.5, heights.first + halfHeight, 0.5)
        return minion.entity.world
            .getNearbyEntities(center, radius + 0.5, halfHeight, radius + 0.5) { type.isInstance(it) }
            .map { type.cast(it) }
            .filter { entity ->
                val block = entity.location.block
                abs(block.x - base.x) <= radius &&
                    abs(block.z - base.z) <= radius &&
                    (block.y - base.y) in heights
            }
    }
}
