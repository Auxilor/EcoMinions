package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionTypeFixture
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionDropTablesTest {

    private val location = mockk<Location>()
    private val type = MinionTypeFixture.build()

    @Test
    fun `resolves drops through the supplied loot table`() {
        var calls = 0
        val tables = MinionDropTables { _, _ ->
            calls++
            listOf(ItemStack(Material.RAW_IRON, 1))
        }

        val drops = tables.dropsFor(type, Material.IRON_ORE, location)

        assertEquals(listOf(ItemStack(Material.RAW_IRON, 1)), drops)
        assertEquals(1, calls)
    }

    @Test
    fun `caches per type and material so the loot table is read once`() {
        var calls = 0
        val tables = MinionDropTables { _, _ ->
            calls++
            listOf(ItemStack(Material.RAW_IRON, 1))
        }

        repeat(5) { tables.dropsFor(type, Material.IRON_ORE, location) }

        assertEquals(1, calls)
    }

    @Test
    fun `falls back to the material itself when no loot table resolves`() {
        val tables = MinionDropTables { _, _ -> emptyList() }

        val drops = tables.dropsFor(type, Material.IRON_ORE, location)

        assertEquals(listOf(ItemStack(Material.IRON_ORE, 1)), drops)
    }

    @Test
    fun `returns copies so callers cannot mutate the cache`() {
        val tables = MinionDropTables { _, _ -> listOf(ItemStack(Material.RAW_IRON, 1)) }

        tables.dropsFor(type, Material.IRON_ORE, location).first().amount = 64
        val second = tables.dropsFor(type, Material.IRON_ORE, location)

        assertEquals(1, second.first().amount)
    }
}
