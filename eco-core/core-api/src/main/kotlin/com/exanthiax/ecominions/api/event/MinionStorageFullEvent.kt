package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a minion's storage fills up.
 *
 * Fires on the change into the full state, not on every tick spent full. Not
 * cancellable.
 */
class MinionStorageFullEvent(
    override val minion: Minion
) : Event(), MinionEvent {

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
