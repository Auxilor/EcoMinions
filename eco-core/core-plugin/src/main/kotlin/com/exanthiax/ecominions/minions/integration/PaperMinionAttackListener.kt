package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Opens a minion when it is hit. An invulnerable stand never fires a damage event, so
 * this listens before the attack lands. Spigot never loads [PrePlayerAttackEntityEvent];
 * only register when [isSupported].
 */
class PaperMinionAttackListener(
    private val minions: MinionService,
    private val placementListener: MinionPlacementListener
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onAttack(event: PrePlayerAttackEntityEvent) {
        val stand = event.attacked as? ArmorStand ?: return
        val minion = minions.getByEntity(stand) ?: return

        event.isCancelled = true
        placementListener.openIfAllowed(event.player, minion)
    }

    companion object {
        val isSupported: Boolean = runCatching {
            Class.forName("io.papermc.paper.event.player.PrePlayerAttackEntityEvent")
        }.isSuccess
    }
}
