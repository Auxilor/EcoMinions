package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

/** Loads minions back out of the world as their chunks come and go. */
class MinionChunkListener(
    private val minions: MinionService
) : Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        minions.hydrateChunk(event.chunk)
    }

    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        minions.dehydrateChunk(event.chunk)
    }
}
