package com.exanthiax.ecominions.core.event

import org.bukkit.event.Cancellable
import org.bukkit.event.Event

/** Records dispatched events, runs [edit] on each, and cancels them all unless [allow]. */
class RecordingEventDispatcher(
    private val allow: Boolean = true,
    private val edit: (Event) -> Unit = {}
) : EventDispatcher {

    private val recorded = mutableListOf<Event>()

    val events: List<Event>
        get() = recorded

    override fun dispatch(event: Event): Boolean {
        recorded += event
        edit(event)
        if (!allow && event is Cancellable) {
            event.isCancelled = true
        }
        return event !is Cancellable || !event.isCancelled
    }

    inline fun <reified T : Event> last(): T = events.filterIsInstance<T>().last()
}
