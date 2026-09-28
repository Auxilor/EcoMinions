package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.block.Block
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * Fired before a minion breaks a block.
 *
 * Fires once per block per minion, so it is only built when something listens.
 * A listener may replace [drops], which then pass through the minion's effects
 * as normal, or cancel to leave the block standing.
 */
class MinionMineEvent(
    override val minion: Minion,
    val block: Block,
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
