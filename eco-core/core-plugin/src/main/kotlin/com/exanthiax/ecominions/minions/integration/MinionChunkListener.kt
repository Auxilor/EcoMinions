package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.event.world.EntitiesUnloadEvent

/** Loads minions back out of the world as their chunks' entities come and go. */
class MinionChunkListener(
    private val minions: MinionService
) : Listener {

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        minions.hydrateEntities(event.entities)
    }

    @EventHandler
    fun onEntitiesUnload(event: EntitiesUnloadEvent) {
        minions.dehydrateEntities(event.entities)
    }
}
