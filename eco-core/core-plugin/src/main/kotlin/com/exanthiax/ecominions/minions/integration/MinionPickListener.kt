package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCreativeEvent

/**
 * Stops creative pick-block on a minion handing out a plain armor stand.
 *
 * Spigot has no pick event, so this catches the resulting creative slot update
 * instead. Paper and its forks are handled by [PaperMinionPickListener].
 */
class MinionPickListener(
    private val minions: MinionService
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onCreativePick(event: InventoryCreativeEvent) {
        if (event.cursor.type != Material.ARMOR_STAND) {
            return
        }
        val player = event.whoClicked as? Player ?: return
        val stand = targetedStand(player) ?: return
        minions.getByEntity(stand) ?: return

        event.isCancelled = true
    }

    private fun targetedStand(player: Player): ArmorStand? {
        val eye = player.eyeLocation
        val hit = player.world.rayTrace(
            eye,
            eye.direction,
            PICK_RANGE,
            FluidCollisionMode.NEVER,
            true,
            0.0
        ) { it != player } ?: return null
        return hit.hitEntity as? ArmorStand
    }

    private companion object {
        const val PICK_RANGE = 6.0
    }
}
