package com.exanthiax.ecominions.core.event

import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

/**
 * Fires plugin events.
 *
 * A seam so that every cancellable action can be tested both ways without a
 * running server.
 */
fun interface EventDispatcher {
    /** @return True if the event was not cancelled and the caller may go ahead. */
    fun dispatch(event: Event): Boolean
}

/** The real dispatcher, used everywhere outside tests. */
object BukkitEventDispatcher : EventDispatcher {
    override fun dispatch(event: Event): Boolean {
        Bukkit.getPluginManager().callEvent(event)
        return event !is Cancellable || !event.isCancelled
    }
}
