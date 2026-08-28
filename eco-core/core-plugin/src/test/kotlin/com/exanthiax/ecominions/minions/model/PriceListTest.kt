package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.testsupport.FakeItemLookup
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PriceListTest {

    private fun vanilla(material: Material, price: Double) = PriceList.Entry(
        item = FakeItemLookup.testable(material),
        price = price,
        vanillaMaterial = material
    )

    /** An entry eco resolved but no material names, as a custom item would be. */
    private fun custom(matching: Material, price: Double) = PriceList.Entry(
        item = FakeItemLookup.testable(matching),
        price = price,
        vanillaMaterial = null
    )

    @Test
    fun `prices a listed item`() {
        val prices = PriceList(listOf(vanilla(Material.COAL, 2.5), vanilla(Material.IRON_INGOT, 7.0)))

        assertEquals(2.5, prices.priceOf(ItemStack(Material.COAL)))
    }

    @Test
    fun `has no price for an unlisted item`() {
        assertNull(PriceList(listOf(vanilla(Material.COAL, 2.5))).priceOf(ItemStack(Material.IRON_INGOT)))
    }

    @Test
    fun `prices a custom item by matching rather than by material`() {
        val prices = PriceList(listOf(custom(Material.DIAMOND, 99.0)))

        assertEquals(99.0, prices.priceOf(ItemStack(Material.DIAMOND)))
    }

    @Test
    fun `prefers the material index over scanning custom entries`() {
        val prices = PriceList(listOf(custom(Material.COAL, 99.0), vanilla(Material.COAL, 2.5)))

        assertEquals(2.5, prices.priceOf(ItemStack(Material.COAL)))
    }

    @Test
    fun `an empty list prices nothing`() {
        assertNull(PriceList.EMPTY.priceOf(ItemStack(Material.COAL)))
        assertTrue(PriceList.EMPTY.isEmpty)
    }

    @Test
    fun `a populated list is not empty`() {
        assertFalse(PriceList(listOf(vanilla(Material.COAL, 1.0))).isEmpty)
    }
}
