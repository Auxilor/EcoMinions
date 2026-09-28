package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.MinionDefinition
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * Fired before minion items are given to a player.
 *
 * No minion exists yet, so this carries the type instead. A listener may change
 * [level] or [amount].
 */
class MinionGiveEvent(
    player: Player,
    val type: MinionDefinition,
    var level: Int,
    var amount: Int
) : PlayerEvent(player), Cancellable {

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
