package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.Trigger
import org.bukkit.Location
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fires the cancellable event and the libreforge trigger around a feeder or breeder's work. */
class ActivitySignals(
    private val events: EventDispatcher,
    private val dispatch: MinionTriggerDispatch,
    private val listening: (HandlerList) -> Boolean = { it.registeredListeners.isNotEmpty() }
) {
    /** @return False when a listener cancelled [event]. Not fired at all when nothing listens on [handlers]. */
    fun allows(event: Event, handlers: HandlerList): Boolean = !listening(handlers) || events.dispatch(event)

    fun trigger(minion: Minion, trigger: Trigger, location: Location, value: Double) {
        dispatch.triggerAt(minion, trigger, location, value)
    }
}
