package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.fuel.Fuel
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired when the fuel a minion was burning runs out. Not cancellable. */
class MinionFuelExpireEvent(
    override val minion: Minion,
    val fuel: Fuel
) : Event(), MinionEvent {

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
