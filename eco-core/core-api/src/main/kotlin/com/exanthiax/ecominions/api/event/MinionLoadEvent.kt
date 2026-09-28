package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a placed minion is loaded back in, because its chunk loaded or the
 * server started. Not fired for a newly placed minion.
 */
class MinionLoadEvent(
    override val minion: Minion
) : Event(), MinionEvent {

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
