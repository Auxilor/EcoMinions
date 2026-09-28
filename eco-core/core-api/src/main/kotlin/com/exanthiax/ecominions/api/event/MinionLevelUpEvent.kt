package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired once per level a minion gains, after its state and stats are updated. */
class MinionLevelUpEvent(
    override val minion: Minion,
    val previousLevel: Int,
    val newLevel: Int
) : Event(), MinionEvent {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
