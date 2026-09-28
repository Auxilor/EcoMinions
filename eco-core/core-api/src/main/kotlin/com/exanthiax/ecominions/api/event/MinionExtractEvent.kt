package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

/**
 * Fired before a player takes items out of a minion's storage.
 *
 * [items] is what will be taken out of storage and handed over. A listener may
 * remove entries or lower their amounts to leave items behind; anything not in
 * storage is ignored.
 */
class MinionExtractEvent(
    player: Player,
    override val minion: Minion,
    var items: MutableList<ItemStack>
) : PlayerEvent(player), Cancellable, MinionEvent {

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
