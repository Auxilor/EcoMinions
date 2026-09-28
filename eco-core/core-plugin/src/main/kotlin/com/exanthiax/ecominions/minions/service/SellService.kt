package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionSellEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
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
    private val shop: ShopIntegrationService,
    private val events: EventDispatcher
) {
    /**
     * Empties every sellable stack and pays the owner.
     *
     * Quotes every stack before selling any, so the sell event sees the whole
     * sale and cancelling it leaves storage untouched.
     *
     * @return How much was paid through the server economy. EcoShop sales settle
     *         in their own currency and are not counted here.
     */
    fun sellStorage(minion: Minion): Double {
        val owner = minion.state.owner ?: return 0.0
        val player = Bukkit.getPlayer(owner) ?: return 0.0
        val type = minion.state.definition

        val sales = (0 until minion.storage.size).mapNotNull { slot ->
            val item = minion.storage.getItem(slot) ?: return@mapNotNull null
            pricing.quote(type, item, item.amount)?.let { quote -> slot to quote }
        }
        if (sales.isEmpty()) {
            return 0.0
        }

        val event = MinionSellEvent(
            minion,
            sales.sumOf { (_, quote) -> (quote as? SellPricingService.Quote.Configured)?.totalValue ?: 0.0 },
            sales.mapNotNull { (slot, _) -> minion.storage.getItem(slot)?.clone() }
        )
        if (!events.dispatch(event)) {
            return 0.0
        }

        for ((slot, quote) in sales) {
            when (quote) {
                is SellPricingService.Quote.Configured -> minion.storage.setItem(slot, null)

                is SellPricingService.Quote.Shop -> {
                    val item = minion.storage.getItem(slot) ?: continue
                    if (shop.sell(player, item, quote.units, quote.multiplier)) {
                        minion.storage.setItem(slot, null)
                    }
                }
            }
        }

        if (event.total > 0.0) {
            EconomyManager.giveMoney(Bukkit.getOfflinePlayer(owner), event.total)
        }
        return event.total
    }
}
