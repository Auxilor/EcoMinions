package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SellPricingServiceTest {

    private fun service(
        autoFromEcoShop: Boolean = true,
        multiplier: Double = 1.0,
        shopUnitsFor: (Material) -> Int? = { null }
    ): SellPricingService {
        val settings = SettingsHolder(SellPricingSettings(autoFromEcoShop, multiplier))
        return SellPricingService(settings, shopUnitsFor)
    }

    private val type = MinionTypeFixture.build(sellPrices = mapOf(Material.COAL to 5.0))

    @Test
    fun `a configured price wins and ignores the multiplier`() {
        val quote = service(multiplier = 10.0).quote(type, Material.COAL, amount = 3)

        assertEquals(SellPricingService.Quote.Configured(15.0), quote)
    }

    @Test
    fun `a shop price is used when nothing is configured`() {
        val quote = service(shopUnitsFor = { 4 }).quote(type, Material.IRON_INGOT, amount = 2)

        assertEquals(SellPricingService.Quote.Shop(units = 2, multiplier = 1.0), quote)
    }

    @Test
    fun `the multiplier applies to shop prices`() {
        val quote = service(multiplier = 0.5, shopUnitsFor = { 4 })
            .quote(type, Material.IRON_INGOT, amount = 2)

        assertEquals(SellPricingService.Quote.Shop(units = 2, multiplier = 0.5), quote)
    }

    @Test
    fun `disabling auto pricing falls back to unsellable`() {
        val quote = service(autoFromEcoShop = false, shopUnitsFor = { 4 })
            .quote(type, Material.IRON_INGOT, amount = 2)

        assertNull(quote)
    }

    @Test
    fun `a material with neither price is unsellable`() {
        assertNull(service().quote(type, Material.IRON_INGOT, amount = 2))
    }

    @Test
    fun `isSellable reports configured materials`() {
        assertTrue(service().isSellable(type, Material.COAL))
    }

    @Test
    fun `isSellable reports shop backed materials`() {
        assertTrue(service(shopUnitsFor = { 4 }).isSellable(type, Material.IRON_INGOT))
    }

    @Test
    fun `isSellable rejects a material neither source prices`() {
        assertFalse(service().isSellable(type, Material.IRON_INGOT))
    }
}
