package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.OfflinePlayer
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired before a minion's owner changes.
 *
 * Either owner may be null, for an unowned minion. A listener may redirect the
 * change by setting [newOwner].
 */
class MinionOwnerChangeEvent(
    override val minion: Minion,
    val oldOwner: OfflinePlayer?,
    var newOwner: OfflinePlayer?
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
