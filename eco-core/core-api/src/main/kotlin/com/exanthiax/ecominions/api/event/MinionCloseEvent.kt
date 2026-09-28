package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** Fired after a player closes a minion menu. Not cancellable. */
class MinionCloseEvent(
    player: Player,
    override val minion: Minion,
    val menuType: MinionMenuType
) : PlayerEvent(player), MinionEvent {

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
