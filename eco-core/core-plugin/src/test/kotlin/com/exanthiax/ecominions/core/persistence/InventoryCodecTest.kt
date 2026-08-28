package com.exanthiax.ecominions.core.persistence

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Covers what [InventoryCodec] decides to write. The bytes themselves are not
 * round-tripped here: `ItemStack.serialize` reaches for a running server, so a
 * non-null stack cannot be encoded outside one.
 */
class InventoryCodecTest {

    @Test
    fun `trailing empty slots are not written`() {
        val items = arrayOfNulls<ItemStack>(512)
        items[0] = ItemStack(Material.COBBLESTONE, 1)

        assertEquals(1, InventoryCodec.occupiedLength(items))
    }

    @Test
    fun `interior empty slots keep their positions`() {
        val items = arrayOfNulls<ItemStack>(4)
        items[0] = ItemStack(Material.COBBLESTONE, 1)
        items[2] = ItemStack(Material.DIAMOND, 1)

        assertEquals(3, InventoryCodec.occupiedLength(items))
    }

    @Test
    fun `an entirely empty array round-trips as empty`() {
        val decoded = InventoryCodec.decode(InventoryCodec.encode(arrayOfNulls(64)))

        assertEquals(0, decoded.size)
    }
}
