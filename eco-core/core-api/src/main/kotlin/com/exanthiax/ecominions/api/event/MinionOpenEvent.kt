package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** Fired before a minion menu is shown to a player. */
class MinionOpenEvent(
    player: Player,
    override val minion: Minion,
    val menuType: MinionMenuType
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
