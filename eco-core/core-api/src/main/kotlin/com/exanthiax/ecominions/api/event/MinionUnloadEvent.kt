package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when a minion is saved and unloaded because its chunk unloaded. */
class MinionUnloadEvent(
    override val minion: Minion
) : Event(), MinionEvent {

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
