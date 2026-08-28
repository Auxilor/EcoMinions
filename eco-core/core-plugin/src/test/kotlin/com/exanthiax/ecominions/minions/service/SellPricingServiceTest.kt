package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.PriceList
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SellPricingServiceTest {

    private fun service(
        autoFromEcoShop: Boolean = true,
        multiplier: Double = 1.0,
        shopUnitsFor: (ItemStack) -> Int? = { null }
    ): SellPricingService {
        val settings = SettingsHolder(SellPricingSettings(autoFromEcoShop, multiplier))
        return SellPricingService(settings, shopUnitsFor)
    }

    private val type = MinionDefinitionFixture.build(
        prices = PriceList(
            listOf(
                PriceList.Entry(
                    item = FakeItemLookup.testable(Material.COAL),
                    price = 5.0,
                    vanillaMaterial = Material.COAL
                )
            )
        )
    )

    @Test
    fun `a configured price wins and ignores the multiplier`() {
        val quote = service(multiplier = 10.0).quote(type, ItemStack(Material.COAL), amount = 3)

        assertEquals(SellPricingService.Quote.Configured(15.0), quote)
    }

    @Test
    fun `a shop price is used when nothing is configured`() {
        val quote = service(shopUnitsFor = { 4 }).quote(type, ItemStack(Material.IRON_INGOT), amount = 2)

        assertEquals(SellPricingService.Quote.Shop(units = 2, multiplier = 1.0), quote)
    }

    @Test
    fun `the multiplier applies to shop prices`() {
        val quote = service(multiplier = 0.5, shopUnitsFor = { 4 })
            .quote(type, ItemStack(Material.IRON_INGOT), amount = 2)

        assertEquals(SellPricingService.Quote.Shop(units = 2, multiplier = 0.5), quote)
    }

    @Test
    fun `disabling auto pricing falls back to unsellable`() {
        val quote = service(autoFromEcoShop = false, shopUnitsFor = { 4 })
            .quote(type, ItemStack(Material.IRON_INGOT), amount = 2)

        assertNull(quote)
    }

    @Test
    fun `a material with neither price is unsellable`() {
        assertNull(service().quote(type, ItemStack(Material.IRON_INGOT), amount = 2))
    }

    @Test
    fun `isSellable reports configured materials`() {
        assertTrue(service().isSellable(type, ItemStack(Material.COAL)))
    }

    @Test
    fun `isSellable reports shop backed materials`() {
        assertTrue(service(shopUnitsFor = { 4 }).isSellable(type, ItemStack(Material.IRON_INGOT)))
    }

    @Test
    fun `isSellable rejects a material neither source prices`() {
        assertFalse(service().isSellable(type, ItemStack(Material.IRON_INGOT)))
    }
}
