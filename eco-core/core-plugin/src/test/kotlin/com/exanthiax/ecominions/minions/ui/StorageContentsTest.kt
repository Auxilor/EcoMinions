package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.minions.model.MinionStorage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageContentsTest {

    private fun storage(vararg items: ItemStack): MinionStorage {
        val storage = MinionStorage(64) { a, b -> a.type == b.type }
        items.forEach { storage.add(it) }
        return storage
    }

    @Test
    fun `counts are summed per material and ordered by size`() {
        val contents = StorageContents.top(
            storage(
                ItemStack(Material.DIAMOND, 2),
                ItemStack(Material.COBBLESTONE, 64),
                ItemStack(Material.COBBLESTONE, 10)
            ),
            limit = 9
        )

        assertEquals(listOf(Material.COBBLESTONE to 74, Material.DIAMOND to 2), contents)
    }

    @Test
    fun `an item with no sell price is still shown`() {
        val contents = StorageContents.top(storage(ItemStack(Material.DIRT, 3)), limit = 9)

        assertEquals(listOf(Material.DIRT to 3), contents)
    }

    @Test
    fun `the limit keeps the largest entries`() {
        val contents = StorageContents.top(
            storage(
                ItemStack(Material.DIRT, 1),
                ItemStack(Material.DIAMOND, 5),
                ItemStack(Material.COBBLESTONE, 9)
            ),
            limit = 2
        )

        assertEquals(listOf(Material.COBBLESTONE to 9, Material.DIAMOND to 5), contents)
    }

    @Test
    fun `empty storage shows nothing`() {
        assertTrue(StorageContents.top(storage(), limit = 9).isEmpty())
    }
}
