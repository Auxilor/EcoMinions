package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired after a minion has been placed in the world. */
class MinionPlaceEvent(
    val minion: Minion,
    val player: Player
) : Event() {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
