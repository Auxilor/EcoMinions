package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionDepositEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.accepts
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Moves items a player hands to a feeder or breeder into its storage.
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
        val fuel = fuels.matching(item)
        val feeds = fuel != null && definition.feeder?.accepts(fuel) == true
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
}
