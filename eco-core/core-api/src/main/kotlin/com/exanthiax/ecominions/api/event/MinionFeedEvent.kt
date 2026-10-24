package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.fuel.Fuel
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired before a feeder takes fuel from its storage to refuel [target].
 *
 * [minion] is the feeder. Cancelling leaves the fuel in storage and [target] unfed. A listener may
 * change [durationMillis], which is the burn time handed to the target.
 */
class MinionFeedEvent(
    override val minion: Minion,
    val target: Minion,
    val fuel: Fuel,
    var durationMillis: Long
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
