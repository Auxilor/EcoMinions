package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropContext
import com.willfp.libreforge.triggers.event.DropResult
import com.willfp.libreforge.triggers.event.EditableDropEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The owning player, when they are online.
 *
 * Null otherwise, which is what stock player-targeting effects expect - they
 * no-op rather than misfire. A minion's behaviour therefore differs between an
 * online and offline owner, which is documented rather than hidden.
 */
fun Minion.ownerPlayer(): Player? = state.owner?.let { Bukkit.getPlayer(it) }

/**
 * Runs [drops] through a trigger as an editable drop event, and returns what the
 * effects left behind.
 *
 * Using libreforge's own drop event is the whole point: every stock drop effect
 * reads it, so autosmelt and multiply_drops work on minions without EcoMinions
 * writing a line of drop-handling logic.
 */
fun Minion.dispatchDrops(
    trigger: Trigger,
    drops: List<ItemStack>,
    block: Block?,
    location: Location,
    cause: DropCause,
    value: Double = 1.0
): List<DropResult> {
    val player = ownerPlayer()

    val event = EditableDropEvent(
        initialDrops = drops,
        cause = cause,
        context = DropContext(player = player, block = block),
        dropLocation = location
    )

    trigger.dispatch(
        toDispatcher(),
        TriggerData(
            dispatcher = toDispatcher(),
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

/** Dispatches [trigger] against this minion with no drops attached. */
fun Minion.dispatchTrigger(
    trigger: Trigger,
    block: Block? = null,
    value: Double = 1.0
) {
    trigger.dispatch(
        toDispatcher(),
        TriggerData(
            dispatcher = toDispatcher(),
            player = ownerPlayer(),
            block = block,
            location = block?.location ?: location,
            value = value
        )
    )
}
