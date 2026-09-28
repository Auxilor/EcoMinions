package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCreativeEvent

/**
 * Stops creative pick-block on a minion handing out a plain armor stand.
 *
 * Spigot (and Paper before 1.21.4) has no pick event, so this catches the
 * resulting creative slot update instead. Newer Paper and its forks are handled
 * by [PaperMinionPickListener].
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
        val stand = player.targetedArmorStand(PICK_RANGE) ?: return
        minions.getByEntity(stand) ?: return

        event.isCancelled = true
    }

    private companion object {
        const val PICK_RANGE = 6.0
    }
}
