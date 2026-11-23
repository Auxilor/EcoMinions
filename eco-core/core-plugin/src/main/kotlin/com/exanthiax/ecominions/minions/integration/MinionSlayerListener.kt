package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.event.MinionLoadEvent
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.activity.SlayerActivity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDeathEvent

/** Wires slayer kills and spawned-mob bookkeeping to Bukkit events. */
class MinionSlayerListener(
    private val slayer: SlayerActivity,
    private val minions: MinionService
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: EntityDeathEvent) {
        if (!slayer.onDeath(event)) {
            slayer.forget(event.entity, minions::getByUuid)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onCombust(event: EntityCombustEvent) {
        if (slayer.isSpawned(event.entity)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onLoad(event: MinionLoadEvent) {
        (event.minion as? Minion)?.let(slayer::recount)
    }

    companion object {
        /** The listener keeping slayer mob counts in step, for this platform. */
        fun trackingListener(slayer: SlayerActivity, minions: MinionService): Listener =
            if (PaperMinionSlayerTrackingListener.isSupported) {
                PaperMinionSlayerTrackingListener(slayer, minions)
            } else {
                SpigotMinionSlayerTrackingListener(slayer, minions)
            }
    }
}
