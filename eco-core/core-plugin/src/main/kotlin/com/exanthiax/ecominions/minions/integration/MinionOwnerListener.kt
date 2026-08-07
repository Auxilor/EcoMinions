package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Starts and stops a player's minions as they come and go.
 *
 * Minions do nothing at all while their owner is offline; the absence is credited
 * in one go when they come back.
 */
class MinionOwnerListener(
    private val minions: MinionService,
    private val activity: MinionActivityService
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val now = System.currentTimeMillis()
        for (minion in minions.ownedMinions(event.player.uniqueId)) {
            activity.resume(minion, now)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val now = System.currentTimeMillis()
        for (minion in minions.ownedMinions(event.player.uniqueId)) {
            activity.markIdle(minion, now)
        }
    }
}
