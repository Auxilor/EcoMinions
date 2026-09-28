package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.util.BoundingBox

/**
 * Stops pistons shoving minions off their spot.
 *
 * Bukkit has no event for an entity being pushed, so any piston move whose
 * blocks would sweep through a minion is cancelled outright.
 */
class MinionPistonListener(
    private val minions: MinionService
) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExtend(event: BlockPistonExtendEvent) {
        val head = event.block.getRelative(event.direction)
        if (sweepsMinion(event, event.blocks + head)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onRetract(event: BlockPistonRetractEvent) {
        if (sweepsMinion(event, event.blocks)) {
            event.isCancelled = true
        }
    }

    /**
     * Covers every moved block and its neighbours along the piston axis, plus
     * the space above - entities resting on a moving block ride along with it.
     * Both axis neighbours are checked as servers disagree on which way a
     * retract event's direction points.
     */
    private fun sweepsMinion(event: BlockPistonEvent, moved: List<Block>): Boolean {
        val direction = event.direction
        val world = event.block.world

        return moved.any { block ->
            val box = BoundingBox.of(block.getRelative(direction.oppositeFace))
                .union(BoundingBox.of(block.getRelative(direction)))
            box.expand(BlockFace.UP.direction, 1.0)
            world.getNearbyEntities(box) { it is ArmorStand && minions.getByEntity(it) != null }.isNotEmpty()
        }
    }
}
