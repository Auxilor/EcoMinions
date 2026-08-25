package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import org.bukkit.Material

/**
 * Decides what a minion's drop is worth, and which economy pays for it.
 *
 * The EcoShop lookup is injected as a function so this class carries no EcoShop
 * types: the boundary lives in ShopIntegrationService, and this stays testable
 * without either plugin present.
 */
class SellPricingService(
    private val settings: SettingsHolder<SellPricingSettings>,
    /** @return How many shop units [Material] sells for, or null if EcoShop won't sell it. */
    private val shopUnitsFor: (Material) -> Int?
) {
    /** What a stack is worth, and who settles it. */
    sealed interface Quote {
        /** A price from the minion's own config, paid through the server economy. */
        data class Configured(val totalValue: Double) : Quote

        /** A price from EcoShop, settled in whatever currency that shop item names. */
        data class Shop(val units: Int, val multiplier: Double) : Quote
    }

    fun quote(type: MinionDefinition, material: Material, amount: Int): Quote? {
        if (amount <= 0) {
            return null
        }

        type.priceOf(material)?.let {
            return Quote.Configured(it * amount)
        }

        val current = settings.value
        if (!current.autoFromEcoShop) {
            return null
        }
        shopUnitsFor(material) ?: return null

        return Quote.Shop(units = amount, multiplier = current.multiplier)
    }

    /** @return Whether [material] has a price from either source. */
    fun isSellable(type: MinionDefinition, material: Material): Boolean {
        if (type.priceOf(material) != null) {
            return true
        }
        return settings.value.autoFromEcoShop && shopUnitsFor(material) != null
    }
}
