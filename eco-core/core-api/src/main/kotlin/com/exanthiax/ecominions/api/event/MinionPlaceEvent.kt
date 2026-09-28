package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.MinionDefinition
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired before a minion is placed in the world.
 *
 * No minion exists yet, so this carries the type and level instead. [player] is
 * null when the API places a minion with no player involved. A listener may move
 * the minion by changing [location], or give it to someone else by changing
 * [owner].
 */
class MinionPlaceEvent(
    val player: Player?,
    val type: MinionDefinition,
    val level: Int,
    var location: Location,
    var owner: OfflinePlayer?
) : Event(), Cancellable {

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
