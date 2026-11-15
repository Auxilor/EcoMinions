package com.exanthiax.ecominions.minions.service

import org.bukkit.inventory.ItemStack

/**
 * What [handOver] took out of a minion's storage.
 *
 * @property taken How many items left storage before delivery, leftovers included.
 * @property delivered One stack per item that reached its destination, sized to what arrived.
 */
class StorageHandover(val taken: Int, val delivered: List<ItemStack>) {
    val moved: Int
        get() = delivered.sumOf { it.amount }

    companion object {
        val NONE = StorageHandover(0, emptyList())
    }
}

/**
 * Removes each of [items] from storage and passes what was removed to [deliver], which returns how many it
 * could not take. Those go back into storage. Saving is left to the caller.
 */
fun Minion.handOver(items: List<ItemStack>, deliver: (ItemStack) -> Int): StorageHandover {
    var taken = 0
    val delivered = mutableListOf<ItemStack>()
    for (item in items) {
        val removed = storage.remove(item)
        if (removed <= 0) {
            continue
        }
        val undelivered = deliver(item.clone().apply { amount = removed })
        if (undelivered > 0) {
            storage.add(item.clone().apply { amount = undelivered })
        }
        if (removed > undelivered) {
            delivered += item.clone().apply { amount = removed - undelivered }
        }
        taken += removed
    }
    return StorageHandover(taken, delivered)
}
