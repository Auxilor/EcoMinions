package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import com.exanthiax.ecominions.api.upgrade.Upgrade
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired before a minion's upgrade tier changes.
 *
 * [player] is null when the API sets the tier. A listener may change [tier];
 * it is clamped to the upgrade's configured range. Cancelling a purchase
 * charges nothing.
 */
class MinionUpgradeEvent(
    override val minion: Minion,
    val player: Player?,
    val upgrade: Upgrade,
    var tier: Int
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
