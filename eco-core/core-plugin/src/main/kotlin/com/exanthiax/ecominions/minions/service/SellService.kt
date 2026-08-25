package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.integration.shop.ShopIntegrationService
import com.willfp.eco.core.integrations.economy.EconomyManager
import org.bukkit.Bukkit

/**
 * Sells a minion's storage to its owner.
 *
 * Only ever runs for an active minion, so the owner is online and EcoShop's
 * player-scoped pricing, limits and multipliers all apply normally.
 */
class SellService(
    private val pricing: SellPricingService,
    private val shop: ShopIntegrationService
) {
    /**
     * Empties every sellable stack and pays the owner.
     *
     * @return How much was paid through the server economy. EcoShop sales settle
     *         in their own currency and are not counted here.
     */
    fun sellStorage(minion: Minion): Double {
        val owner = minion.state.owner ?: return 0.0
        val player = Bukkit.getPlayer(owner) ?: return 0.0
        val type = minion.state.definition

        var economyTotal = 0.0
        for (slot in 0 until minion.storage.size) {
            val item = minion.storage.getItem(slot) ?: continue

            when (val quote = pricing.quote(type, item, item.amount)) {
                is SellPricingService.Quote.Configured -> {
                    economyTotal += quote.totalValue
                    minion.storage.setItem(slot, null)
                }

                is SellPricingService.Quote.Shop -> {
                    if (shop.sell(player, item, quote.units, quote.multiplier)) {
                        minion.storage.setItem(slot, null)
                    }
                }

                null -> Unit
            }
        }

        if (economyTotal > 0.0) {
            EconomyManager.giveMoney(Bukkit.getOfflinePlayer(owner), economyTotal)
        }
        return economyTotal
    }
}
