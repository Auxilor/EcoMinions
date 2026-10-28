package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.minions.model.MinionStorage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageContentsTest {

    /** Stacks of one material merge, which is what a plain vanilla drop does. */
    private val byMaterial: (ItemStack, ItemStack) -> Boolean = { a, b -> a.type == b.type }

    private fun storage(
        vararg items: ItemStack,
        similar: (ItemStack, ItemStack) -> Boolean = byMaterial
    ): MinionStorage {
        val storage = MinionStorage(64, similar)
        items.forEach { storage.add(it) }
        return storage
    }

    private fun MinionStorage.contents(
        limit: Int,
        similar: (ItemStack, ItemStack) -> Boolean = byMaterial
    ): List<Pair<Material, Int>> =
        StorageContents.top(this, limit, similar).map { (sample, count) -> sample.type to count }

    @Test
    fun `counts are summed per item and ordered by size`() {
        val contents = storage(
            ItemStack(Material.DIAMOND, 2),
            ItemStack(Material.COBBLESTONE, 64),
            ItemStack(Material.COBBLESTONE, 10)
        ).contents(limit = 9)

        assertEquals(listOf(Material.COBBLESTONE to 74, Material.DIAMOND to 2), contents)
    }

    @Test
    fun `an item with no sell price is still shown`() {
        val contents = storage(ItemStack(Material.DIRT, 3)).contents(limit = 9)

        assertEquals(listOf(Material.DIRT to 3), contents)
    }

    @Test
    fun `the limit keeps the largest entries`() {
        val contents = storage(
            ItemStack(Material.DIRT, 1),
            ItemStack(Material.DIAMOND, 5),
            ItemStack(Material.COBBLESTONE, 9)
        ).contents(limit = 2)

        assertEquals(listOf(Material.COBBLESTONE to 9, Material.DIAMOND to 5), contents)
    }

    @Test
    fun `empty storage shows nothing`() {
        assertTrue(storage().contents(limit = 9).isEmpty())
    }

    @Test
    fun `two items of one material that do not stack are shown separately`() {
        // Stands in for a compacted item: same material as the raw drop, but
        // meta that keeps the two from stacking.
        val plain = ItemStack(Material.COBBLESTONE, 5)
        val compacted = ItemStack(Material.COBBLESTONE, 2)
        val identity: (ItemStack, ItemStack) -> Boolean = { a, b -> a === b }

        val contents = storage(plain, compacted, similar = identity).contents(limit = 9, similar = identity)

        assertEquals(listOf(Material.COBBLESTONE to 5, Material.COBBLESTONE to 2), contents)
    }

    @Test
    fun `the sample is a copy of the stored item, not the stored item itself`() {
        val stored = ItemStack(Material.DIAMOND, 4)
        val storage = storage(stored)

        val sample = StorageContents.top(storage, limit = 9, similar = byMaterial).single().first

        assertEquals(Material.DIAMOND, sample.type)
        assertEquals(1, sample.amount)
        assertEquals(4, storage.getItem(0)?.amount)
    }

    @Test
    fun `sample is the raw stored stack`() {
        val storage = storage(ItemStack(Material.DIAMOND, 2), ItemStack(Material.COBBLESTONE, 64))

        val sample = StorageContents.top(storage, 9, byMaterial)[1].first

        assertEquals(Material.DIAMOND, sample.type)
        assertEquals(1, sample.amount)
        assertEquals(1, storage.remove(sample))
        assertEquals(1, storage.getItem(0)?.amount)
    }
}
