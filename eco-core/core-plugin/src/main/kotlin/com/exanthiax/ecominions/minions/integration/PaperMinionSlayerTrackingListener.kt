package com.exanthiax.ecominions.minions.integration

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.activity.SlayerActivity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Keeps slayer mob counts in step as mobs enter and leave the world, on Paper and forks. Kept apart
 * from [SpigotMinionSlayerTrackingListener] so Spigot never loads the Paper events; only register
 * when [isSupported].
 */
class PaperMinionSlayerTrackingListener(
    private val slayer: SlayerActivity,
    private val minions: MinionService
) : Listener {

    @EventHandler
    fun onRemove(event: EntityRemoveFromWorldEvent) {
        slayer.forget(event.entity, minions::getByUuid)
    }

    @EventHandler
    fun onAdd(event: EntityAddToWorldEvent) {
        slayer.track(event.entity, minions::getByUuid)
    }

    companion object {
        val isSupported: Boolean = runCatching {
            Class.forName("com.destroystokyo.paper.event.entity.EntityAddToWorldEvent")
        }.isSuccess
    }
}
