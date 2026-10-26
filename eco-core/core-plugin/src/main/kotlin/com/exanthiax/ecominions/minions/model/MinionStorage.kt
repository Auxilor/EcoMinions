package com.exanthiax.ecominions.minions.model

import org.bukkit.inventory.ItemStack

/**
 * A minion's storage: a fixed number of stack slots it owns outright.
 *
 * Not a Bukkit [org.bukkit.inventory.Inventory], because a chest inventory's
 * size must be a multiple of nine and a minion's capacity is upgraded a slot at
 * a time. Nothing ever opens this container - the main menu renders an
 * aggregated view of it - so there is no reason to pay that constraint.
 *
 * [used] is maintained as slots change rather than recounted, because [isFull]
 * is read on every mine cycle and capacity is no longer bounded at 54.
 *
 * @param similar Whether two stacks may merge. Injected because a unit test's
 *        fake [ItemStack] cannot answer `isSimilar`.
 */
class MinionStorage(
    capacity: Int,
    private val similar: (ItemStack, ItemStack) -> Boolean = { a, b -> a.isSimilar(b) }
) {
    private var slots: Array<ItemStack?> = arrayOfNulls(capacity.coerceAtLeast(1))

    var used: Int = 0
        private set

    val size: Int
        get() = slots.size

    val isFull: Boolean
        get() = used >= slots.size

    fun getItem(index: Int): ItemStack? = slots.getOrNull(index)

    fun setItem(index: Int, item: ItemStack?) {
        if (index !in slots.indices) {
            return
        }
        val previous = slots[index]
        slots[index] = item

        if (previous == null && item != null) {
            used++
        } else if (previous != null && item == null) {
            used--
        }
    }

    /**
     * Merges [item] into matching partial stacks, then fills empty slots.
     *
     * @return What did not fit, or null if all of it did. The argument is never
     *         mutated: the remainder is a separate stack.
     */
    fun add(item: ItemStack): ItemStack? {
        var remaining = item.amount
        val limit = item.type.maxStackSize

        for (index in slots.indices) {
            if (remaining <= 0) {
                break
            }
            val existing = slots[index] ?: continue
            if (existing.amount >= limit || !similar(existing, item)) {
                continue
            }
            val moved = minOf(limit - existing.amount, remaining)
            existing.amount += moved
            remaining -= moved
        }

        for (index in slots.indices) {
            if (remaining <= 0) {
                break
            }
            if (slots[index] != null) {
                continue
            }
            val placed = item.clone()
            placed.amount = minOf(limit, remaining)
            remaining -= placed.amount
            setItem(index, placed)
        }

        if (remaining <= 0) {
            return null
        }

        val leftover = item.clone()
        leftover.amount = remaining
        return leftover
    }

    /**
     * Adds every item it can.
     *
     * Keeps going after one item fails: a full slot for cobblestone says nothing
     * about whether a diamond fits.
     *
     * @return Everything that did not fit.
     */
    fun store(items: List<ItemStack>): List<ItemStack> {
        val leftovers = mutableListOf<ItemStack>()
        for (item in items) {
            val remainder = add(item)
            if (remainder != null) {
                leftovers += remainder
            }
        }
        return leftovers
    }

    /**
     * Removes up to [item]'s amount of stacks similar to it, last slot first so
     * partial stacks left behind stay at the front.
     *
     * @return How many items were removed.
     */
    fun remove(item: ItemStack): Int {
        var remaining = item.amount
        for (index in slots.indices.reversed()) {
            if (remaining <= 0) {
                break
            }
            val existing = slots[index] ?: continue
            if (!similar(existing, item)) {
                continue
            }
            val moved = minOf(existing.amount, remaining)
            remaining -= moved
            if (moved >= existing.amount) {
                setItem(index, null)
            } else {
                existing.amount -= moved
            }
        }
        return item.amount - remaining
    }

    fun clear() {
        slots.fill(null)
        used = 0
    }

    /** @return How many items across every slot [matches] accepts. */
    fun count(matches: (ItemStack) -> Boolean): Int =
        slots.sumOf { stack -> if (stack != null && matches(stack)) stack.amount else 0 }

    /** Index-aligned copy for persistence, empty slots included. */
    fun snapshot(): Array<ItemStack?> = slots.copyOf()

    /** A detached copy with cloned stacks, to try additions without touching this storage. */
    fun copy(): MinionStorage = MinionStorage(slots.size, similar).also { copy ->
        slots.forEachIndexed { index, item -> copy.setItem(index, item?.clone()) }
    }

    /**
     * Grows or shrinks to [capacity], preserving contents in order.
     *
     * @return Items that no longer fit. Returned rather than deleted: the caller
     *         decides whether to drop them in the world.
     */
    fun resize(capacity: Int): List<ItemStack> {
        val target = capacity.coerceAtLeast(1)
        if (target == slots.size) {
            return emptyList()
        }

        val contents = slots.filterNotNull()
        slots = arrayOfNulls(target)
        used = 0

        val kept = contents.take(target)
        kept.forEachIndexed { index, item -> setItem(index, item) }

        return contents.drop(target)
    }
}
