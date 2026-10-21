package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionLimits
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/** Reads a joining player's permission caps, so their minions rank against them while they are offline later. */
class MinionLimitsListener(
    private val limits: MinionLimits
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        limits.refreshCaps(event.player)
        limits.invalidate(event.player.uniqueId)
    }
}
