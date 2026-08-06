package com.exanthiax.ecominions.minions.service

import com.willfp.eco.core.integrations.economy.EconomyManager
import org.bukkit.Bukkit

/** Sells a minion's storage to its owner's balance. */
class SellService {

    /**
     * Empties every sellable stack and pays the owner.
     *
     * @return How much the contents were worth; zero if there was nothing to sell.
     */
    fun sellStorage(minion: Minion): Double {
        val owner = minion.state.owner ?: return 0.0
        val prices = minion.state.type.sellPrices
        if (prices.isEmpty()) {
            return 0.0
        }

        var total = 0.0
        for (slot in 0 until minion.storage.size) {
            val item = minion.storage.getItem(slot) ?: continue
            val price = prices[item.type] ?: continue
            total += price * item.amount
            minion.storage.setItem(slot, null)
        }

        if (total > 0.0) {
            EconomyManager.giveMoney(Bukkit.getOfflinePlayer(owner), total)
        }
        return total
    }
}
