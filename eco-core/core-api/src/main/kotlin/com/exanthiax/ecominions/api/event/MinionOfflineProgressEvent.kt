package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * Fired before a minion is credited for the time it spent idle.
 *
 * [drops] is what it would have mined, before its drop effects run. A listener
 * may replace it. Cancelling credits nothing and burns no fuel.
 */
class MinionOfflineProgressEvent(
    override val minion: Minion,
    val idleMillis: Long,
    var drops: MutableList<ItemStack>
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
