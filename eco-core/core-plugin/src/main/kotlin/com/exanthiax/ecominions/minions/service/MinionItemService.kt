package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionGiveEvent
import com.exanthiax.ecominions.api.event.MinionTakeEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Moves minion items in and out of players' inventories. */
class MinionItemService(
    private val itemFactory: MinionItemFactory,
    private val events: EventDispatcher
) {
    /**
     * Gives [player] [amount] items of [type] at [level], dropping what does not
     * fit at their feet.
     *
     * @return How many were given, dropped ones included.
     */
    fun give(player: Player, type: MinionDefinition, level: Int, amount: Int): Int {
        val event = MinionGiveEvent(player, type, level, amount)
        if (!events.dispatch(event) || event.amount <= 0) {
            return 0
        }

        val item = itemFactory.build(MinionState(definition = type, storedLevel = event.level))
        item.amount = event.amount
        for (leftover in player.inventory.addItem(item).values) {
            player.world.dropItemNaturally(player.location, leftover)
        }
        return event.amount
    }

    /** @return How many items of [type] were taken, which may be fewer than [amount]. */
    fun take(player: Player, type: MinionDefinition, amount: Int): Int {
        val event = MinionTakeEvent(player, type, amount)
        if (!events.dispatch(event) || event.amount <= 0) {
            return 0
        }

        val inventory = player.inventory
        var remaining = event.amount
        for (slot in 0 until inventory.size) {
            if (remaining <= 0) {
                break
            }
            val stack = inventory.getItem(slot) ?: continue
            if (!isOfType(stack, type)) {
                continue
            }
            val taken = minOf(stack.amount, remaining)
            remaining -= taken
            if (taken >= stack.amount) {
                inventory.setItem(slot, null)
            } else {
                stack.amount -= taken
                inventory.setItem(slot, stack)
            }
        }
        return event.amount - remaining
    }

    /** @return How many items of [type] [player] is carrying. */
    fun count(player: Player, type: MinionDefinition): Int =
        player.inventory.contents.sumOf { stack ->
            if (stack != null && isOfType(stack, type)) stack.amount else 0
        }

    private fun isOfType(stack: ItemStack, type: MinionDefinition): Boolean =
        itemFactory.read(stack)?.definition?.id == type.id
}
