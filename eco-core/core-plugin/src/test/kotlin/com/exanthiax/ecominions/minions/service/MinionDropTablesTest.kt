package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionDropTablesTest {

    private val location = mockk<Location>()
    private val type = MinionDefinitionFixture.build()

    private val ironOre = MinionBlock.regenDefault(
        id = "iron_ore",
        lookup = "IRON_ORE",
        block = FakeBlockLookup.testable(Material.IRON_ORE)
    ).copy(vanillaMaterial = Material.IRON_ORE)

    /** A custom block: eco resolves it, but no material names it. */
    private fun custom(drops: List<String>) = MinionBlock.regenDefault(
        id = "ruby_ore",
        lookup = "itemsadder:ruby_ore",
        block = FakeBlockLookup.testable(Material.DIAMOND_ORE)
    ).copy(drops = drops)

    private fun tables(
        resolve: (Material, Location) -> List<ItemStack> = { _, _ -> emptyList() },
        resolveItem: (String) -> ItemStack? = { null }
    ) = MinionDropTables(resolve, resolveItem)

    @Test
    fun `resolves drops through the supplied loot table`() {
        var calls = 0
        val tables = tables(resolve = { _, _ ->
            calls++
            listOf(ItemStack(Material.RAW_IRON, 1))
        })

        val drops = tables.dropsFor(type, ironOre, location)

        assertEquals(listOf(ItemStack(Material.RAW_IRON, 1)), drops)
        assertEquals(1, calls)
    }

    @Test
    fun `caches per minion and block so the loot table is read once`() {
        var calls = 0
        val tables = tables(resolve = { _, _ ->
            calls++
            listOf(ItemStack(Material.RAW_IRON, 1))
        })

        repeat(5) { tables.dropsFor(type, ironOre, location) }

        assertEquals(1, calls)
    }

    @Test
    fun `falls back to the block itself when no loot table resolves`() {
        val drops = tables().dropsFor(type, ironOre, location)

        assertEquals(listOf(ItemStack(Material.IRON_ORE, 1)), drops)
    }

    @Test
    fun `returns copies so callers cannot mutate the cache`() {
        val tables = tables(resolve = { _, _ -> listOf(ItemStack(Material.RAW_IRON, 1)) })

        tables.dropsFor(type, ironOre, location).first().amount = 64
        val second = tables.dropsFor(type, ironOre, location)

        assertEquals(1, second.first().amount)
    }

    @Test
    fun `credits a custom block from its declared drops`() {
        val tables = tables(resolveItem = { ItemStack(Material.EMERALD, 1) })

        val drops = tables.dropsFor(type, custom(listOf("itemsadder:ruby")), location)

        assertEquals(listOf(ItemStack(Material.EMERALD, 1)), drops)
    }

    @Test
    fun `credits a custom block nothing when it declares no drops`() {
        val drops = tables().dropsFor(type, custom(emptyList()), location)

        assertTrue(drops.isEmpty(), "a custom block with no drops has no correct value to credit")
    }

    @Test
    fun `skips a declared drop this server does not have`() {
        val tables = tables(resolveItem = { null })

        val drops = tables.dropsFor(type, custom(listOf("missing:item")), location)

        assertTrue(drops.isEmpty())
    }
}
