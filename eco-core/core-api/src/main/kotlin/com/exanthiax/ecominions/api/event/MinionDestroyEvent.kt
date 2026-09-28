package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired before a minion is removed from the world. */
class MinionDestroyEvent(
    override val minion: Minion,
    val cause: DestroyCause
) : Event(), Cancellable, MinionEvent {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
