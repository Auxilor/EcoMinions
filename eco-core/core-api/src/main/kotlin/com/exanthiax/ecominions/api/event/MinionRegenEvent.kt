package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired after a block a minion broke grows back.
 *
 * Only built when something listens. Not cancellable: the block has already
 * been restored.
 */
class MinionRegenEvent(
    override val minion: Minion,
    val block: Block
) : Event(), MinionEvent {

    override fun getHandlers(): HandlerList = getHandlerList()

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }
}
