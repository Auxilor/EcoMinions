package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropContext
import com.willfp.libreforge.triggers.event.DropResult
import com.willfp.libreforge.triggers.event.EditableDropEvent
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * The one path a minion trigger takes.
 *
 * A class rather than the extension functions it replaces, because everything
 * that needs to observe minion triggers - XP gain today - has to be injected,
 * and libreforge's own `TriggerDispatchEvent` is not a usable hook: dispatch
 * returns before firing it when the dispatcher has no triggerable effects and
 * no bound counters, so a type with `xp-gain-methods` but no `effects:` block
 * would silently never gain XP.
 */
class MinionTriggerDispatch(
    private val onDispatch: (Minion, DispatchedTrigger) -> Unit = { _, _ -> }
) {
    /**
     * Runs [drops] through [trigger] as an editable drop event, and returns what
     * the effects left behind.
     *
     * Using libreforge's own drop event is the whole point: every stock drop
     * effect reads it, so autosmelt and multiply_drops work on minions without
     * EcoMinions writing a line of drop-handling logic.
     */
    fun drops(
        minion: Minion,
        trigger: Trigger,
        drops: List<ItemStack>,
        block: Block?,
        location: Location,
        cause: DropCause,
        value: Double = 1.0
    ): List<DropResult> {
        val player = minion.ownerPlayer()

        val event = EditableDropEvent(
            initialDrops = drops,
            cause = cause,
            context = DropContext(player = player, block = block),
            dropLocation = location
        )

        dispatch(
            minion,
            trigger,
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = player,
                block = block,
                event = event,
                location = location,
                item = drops.firstOrNull(),
                value = value
            )
        )

        return event.items
    }

    /** Dispatches [trigger] against [minion] with no drops attached. */
    fun trigger(
        minion: Minion,
        trigger: Trigger,
        block: Block? = null,
        value: Double = 1.0
    ) {
        dispatch(
            minion,
            trigger,
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                block = block,
                location = block?.location ?: minion.location,
                value = value
            )
        )
    }

    private fun dispatch(minion: Minion, trigger: Trigger, data: TriggerData) {
        val dispatcher = minion.toDispatcher()
        trigger.dispatch(dispatcher, data)
        onDispatch(minion, DispatchedTrigger(dispatcher, trigger, data))
    }
}
