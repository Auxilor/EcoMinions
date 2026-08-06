package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.fuel.Fuel
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired after fuel has been applied to a minion. */
class MinionFuelEvent(
    val minion: Minion,
    val player: Player,
    val fuel: Fuel
) : Event() {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
