package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionDepositEvent
import com.exanthiax.ecominions.api.event.MinionExtractEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.accepts
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Moves items a player hands to a feeder or breeder in and out of its storage.
 *
 * Only what the minion's definition accepts goes in, so storage can never hold
 * something the minion has no use for.
 */
class MinionInput(
    private val fuels: FuelService,
    private val save: (Minion) -> Unit,
    private val events: EventDispatcher
) {
    fun takesInput(definition: MinionDefinition): Boolean = definition.feeder != null || definition.breeder != null

    fun accepts(definition: MinionDefinition, item: ItemStack): Boolean {
        val feeds = definition.feeder?.let { settings -> fuels.matching(item)?.let(settings::accepts) } == true
        return feeds || definition.breeder?.accepts(item) == true
    }

    /** What became of a deposit. */
    sealed interface Deposit {
        /** [moved] went in; the caller shrinks the cursor by it. Zero means nothing fit. */
        data class Accepted(val moved: Int) : Deposit

        /** The minion does not take the item. */
        data object Refused : Deposit

        /** A listener cancelled [MinionDepositEvent]. */
        data object Cancelled : Deposit
    }

    /**
     * Fires [MinionDepositEvent] after the accepts check, carrying only the amount that fits.
     * A cancelled event moves nothing, and a full store fires no event.
     */
    fun deposit(player: Player, minion: Minion, item: ItemStack): Deposit {
        val fits = item.amount - (minion.storage.copy().add(item)?.amount ?: 0)
        val event = MinionDepositEvent(player, minion, item.clone().apply { amount = fits })
        return when {
            !accepts(minion.state.definition, item) -> Deposit.Refused
            fits == 0 -> Deposit.Accepted(0)
            !events.dispatch(event) -> Deposit.Cancelled
            else -> Deposit.Accepted(store(minion, item))
        }
    }

    private fun store(minion: Minion, item: ItemStack): Int {
        val moved = item.amount - (minion.storage.add(item)?.amount ?: 0)
        if (moved > 0) {
            save(minion)
        }
        return moved
    }

    /**
     * Moves up to one stack similar to [sample] into [player]'s inventory; what does not fit stays in the minion.
     *
     * Fires [MinionExtractEvent] with the stack about to be taken. Cancelling moves nothing, and only what
     * the listener left in the event's items is taken.
     *
     * @return How many moved.
     */
    fun withdraw(player: Player, minion: Minion, sample: ItemStack): Int {
        val amount = minOf(sample.type.maxStackSize, minion.storage.countSimilar(sample))
        val event = MinionExtractEvent(player, minion, mutableListOf(sample.clone().apply { this.amount = amount }))
        return if (amount > 0 && events.dispatch(event)) hand(player, minion, event.items) else 0
    }

    private fun hand(player: Player, minion: Minion, items: List<ItemStack>): Int {
        var moved = 0
        var taken = 0
        for (item in items) {
            val removed = minion.storage.remove(item)
            val leftover = if (removed > 0) giveBack(player, item, removed) else 0
            if (leftover > 0) {
                minion.storage.add(item.clone().apply { amount = leftover })
            }
            taken += removed
            moved += removed - leftover
        }
        if (taken > 0) {
            save(minion)
        }
        return moved
    }

    private fun giveBack(player: Player, item: ItemStack, removed: Int): Int =
        player.inventory.addItem(item.clone().apply { amount = removed }).values.sumOf { it.amount }
}
