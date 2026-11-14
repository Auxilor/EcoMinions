package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.activity.SlayerActivity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.world.EntitiesLoadEvent

/**
 * Keeps slayer mob counts in step as mobs enter and leave the world, on Spigot. Removal covers
 * unloading too; mobs coming back arrive with their chunk's entities.
 */
class SpigotMinionSlayerTrackingListener(
    private val slayer: SlayerActivity,
    private val minions: MinionService
) : Listener {

    @EventHandler
    fun onRemove(event: EntityRemoveEvent) {
        slayer.forget(event.entity, minions::getByUuid)
    }

    @EventHandler
    fun onLoad(event: EntitiesLoadEvent) {
        for (entity in event.entities) {
            slayer.track(entity, minions::getByUuid)
        }
    }
}
