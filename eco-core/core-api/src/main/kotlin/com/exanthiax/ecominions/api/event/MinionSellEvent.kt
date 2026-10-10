package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * Fired before a minion's storage is sold to its owner.
 *
 * [items] is everything about to be sold. [total] is what the server economy
 * will pay, and a listener may change it to adjust the payout. [shopTotal] is
 * what EcoShop will pay for the items it prices, in each shop item's own
 * currency; EcoShop settles those itself, so it is read-only.
 */
class MinionSellEvent(
    override val minion: Minion,
    var total: Double,
    val items: List<ItemStack>,
    val shopTotal: Double = 0.0
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
