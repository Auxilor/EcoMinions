package com.exanthiax.ecominions.api.minion

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * A handle on one minion's storage.
 *
 * Every change is saved to the minion before the call returns. Items passed in
 * are copied and items handed out are copies, so neither side can change the
 * other's stacks.
 */
interface MinionStorage {
    /** Slots in the storage. */
    val size: Int

    /** Slots currently holding an item. */
    val used: Int

    val isFull: Boolean

    /** @return A copy of every slot, empty slots included. */
    fun contents(): Array<ItemStack?>

    /** @return A copy of the item in [slot], or null if it is empty or out of range. */
    fun getItem(slot: Int): ItemStack?

    /** Replaces the item in [slot]. Does nothing if [slot] is out of range. */
    fun setItem(slot: Int, item: ItemStack?)

    /** @return What did not fit, or null if all of [item] did. */
    fun add(item: ItemStack): ItemStack?

    /**
     * Removes up to [ItemStack.getAmount] items similar to [item].
     *
     * @return How many were removed.
     */
    fun remove(item: ItemStack): Int

    fun clear()

    /** @return How many items of [material] are stored, across every slot. */
    fun countOf(material: Material): Int
}
