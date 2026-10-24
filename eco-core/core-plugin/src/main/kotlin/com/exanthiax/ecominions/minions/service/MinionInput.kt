package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.accepts
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Moves items a player hands to a feeder or breeder in and out of its storage.
 *
 * Only what the minion's definition accepts goes in, so storage can never hold
 * something the minion has no use for.
 */
class MinionInput(
    private val fuels: FuelService,
    private val save: (Minion) -> Unit
) {
    fun takesInput(definition: MinionDefinition): Boolean = definition.feeder != null || definition.breeder != null

    fun accepts(definition: MinionDefinition, item: ItemStack): Boolean {
        val feeds = definition.feeder?.let { settings -> fuels.matching(item)?.let(settings::accepts) } == true
        return feeds || definition.breeder?.accepts(item) == true
    }

    /** @return How many items went in; the caller shrinks the cursor by this. */
    fun deposit(minion: Minion, item: ItemStack): Int {
        if (!accepts(minion.state.definition, item)) {
            return 0
        }
        val moved = item.amount - (minion.storage.add(item)?.amount ?: 0)
        if (moved > 0) {
            save(minion)
        }
        return moved
    }

    /**
     * Moves up to one stack similar to [sample] into [inventory]; what does not fit stays in the minion.
     *
     * @return How many moved.
     */
    fun withdraw(minion: Minion, sample: ItemStack, inventory: Inventory): Int {
        val request = sample.clone().apply { amount = sample.type.maxStackSize }
        val removed = minion.storage.remove(request)
        if (removed <= 0) {
            return 0
        }

        val leftover = inventory.addItem(sample.clone().apply { amount = removed }).values.sumOf { it.amount }
        if (leftover > 0) {
            minion.storage.add(sample.clone().apply { amount = leftover })
        }
        save(minion)
        return removed - leftover
    }
}
