package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.scheduling.Regions
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
 *
 * A player's minions can be anywhere, so each one is handed to the region that
 * owns its stand. One whose chunk unloads before that is already marked idle by
 * the unload, and resumes when it loads again.
 */
class MinionOwnerListener(
    private val minions: MinionService,
    private val activity: MinionActivityService,
    private val regions: Regions
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val now = System.currentTimeMillis()
        for (minion in minions.ownedMinions(event.player.uniqueId)) {
            regions.on(minion.entity) { activity.resume(minion, now) }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val now = System.currentTimeMillis()
        for (minion in minions.ownedMinions(event.player.uniqueId)) {
            regions.on(minion.entity) { activity.markIdle(minion, now) }
        }
    }
}
