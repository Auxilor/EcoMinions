package com.exanthiax.ecominions.minions.model

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionStorageTest {

    /** FakeItemStack does not fake isSimilar, so tests compare by material. */
    private fun storage(capacity: Int) =
        MinionStorage(capacity) { a, b -> a.type == b.type }

    private fun stack(material: Material, amount: Int) = ItemStack(material, amount)

    @Test
    fun `capacity is exact and need not be a multiple of nine`() {
        val storage = storage(14)

        assertEquals(14, storage.size)
        assertEquals(0, storage.used)
        assertFalse(storage.isFull)
    }

    @Test
    fun `adding into empty storage occupies one slot`() {
        val storage = storage(3)

        assertNull(storage.add(stack(Material.COBBLESTONE, 10)))

        assertEquals(1, storage.used)
        assertEquals(10, storage.getItem(0)?.amount)
    }

    @Test
    fun `adding tops up a matching partial stack before using a new slot`() {
        val storage = storage(3)
        storage.add(stack(Material.COBBLESTONE, 60))

        assertNull(storage.add(stack(Material.COBBLESTONE, 4)))

        assertEquals(1, storage.used)
        assertEquals(64, storage.getItem(0)?.amount)
    }

    @Test
    fun `a stack that overflows a partial slot spills into the next slot`() {
        val storage = storage(3)
        storage.add(stack(Material.COBBLESTONE, 60))

        assertNull(storage.add(stack(Material.COBBLESTONE, 10)))

        assertEquals(2, storage.used)
        assertEquals(64, storage.getItem(0)?.amount)
        assertEquals(6, storage.getItem(1)?.amount)
    }

    @Test
    fun `add returns the remainder when storage runs out of room`() {
        val storage = storage(1)
        storage.add(stack(Material.COBBLESTONE, 64))

        val remainder = storage.add(stack(Material.DIAMOND, 5))

        assertEquals(Material.DIAMOND, remainder?.type)
        assertEquals(5, remainder?.amount)
        assertTrue(storage.isFull)
    }

    @Test
    fun `store keeps trying later items after one does not fit`() {
        val storage = storage(2)
        storage.setItem(0, stack(Material.COBBLESTONE, 64))

        val leftover = storage.store(
            listOf(
                stack(Material.DIAMOND, 5),
                stack(Material.EMERALD, 3)
            )
        )

        assertEquals(1, leftover.size)
        assertEquals(Material.EMERALD, leftover.first().type)
        assertEquals(2, storage.used)
        assertEquals(Material.DIAMOND, storage.getItem(1)?.type)
    }

    @Test
    fun `setItem keeps the occupancy count correct in both directions`() {
        val storage = storage(2)

        storage.setItem(0, stack(Material.COBBLESTONE, 1))
        assertEquals(1, storage.used)

        storage.setItem(0, stack(Material.DIAMOND, 1))
        assertEquals(1, storage.used)

        storage.setItem(0, null)
        assertEquals(0, storage.used)
    }

    @Test
    fun `shrinking preserves what fits and returns the overflow`() {
        val storage = storage(3)
        storage.add(stack(Material.COBBLESTONE, 1))
        storage.add(stack(Material.DIAMOND, 1))
        storage.add(stack(Material.EMERALD, 1))

        val overflow = storage.resize(2)

        assertEquals(2, storage.size)
        assertEquals(2, storage.used)
        assertEquals(1, overflow.size)
        assertEquals(Material.EMERALD, overflow.first().type)
    }

    @Test
    fun `growing preserves contents and returns nothing`() {
        val storage = storage(1)
        storage.add(stack(Material.COBBLESTONE, 7))

        val overflow = storage.resize(30)

        assertTrue(overflow.isEmpty())
        assertEquals(30, storage.size)
        assertEquals(1, storage.used)
        assertEquals(7, storage.getItem(0)?.amount)
    }

    @Test
    fun `snapshot is index-aligned and includes empty slots`() {
        val storage = storage(3)
        storage.setItem(1, stack(Material.COBBLESTONE, 2))

        val snapshot = storage.snapshot()

        assertEquals(3, snapshot.size)
        assertNull(snapshot[0])
        assertEquals(Material.COBBLESTONE, snapshot[1]?.type)
        assertNull(snapshot[2])
    }
}
