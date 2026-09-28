package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.fuel.Fuel
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired before fuel is burnt on a minion.
 *
 * Fired once per deposit, so a whole stack of fuel is one event. [player] is
 * null when the API applies fuel. A listener may change [durationMillis] to
 * lengthen or shorten the burn; cancelling leaves the fuel items with the player.
 */
class MinionFuelEvent(
    override val minion: Minion,
    val player: Player?,
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
