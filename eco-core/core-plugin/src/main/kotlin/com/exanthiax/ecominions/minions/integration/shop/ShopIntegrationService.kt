package com.exanthiax.ecominions.minions.integration.shop

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.willfp.ecoshop.shop.SellStatus
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The only place EcoShop is called from.
 *
 * EcoShop's API moves often, so keeping every reference in one file means a
 * breaking change upstream costs one file rather than a sweep. Every method is
 * safe to call when EcoShop is absent.
 */
class ShopIntegrationService(
    private val settings: SettingsHolder<MinionSettings>
) {
    @Volatile
    private var pluginAvailable = false

    fun init() {
        pluginAvailable = Bukkit.getPluginManager().isPluginEnabled("EcoShop")
    }

    fun isEnabled(): Boolean = pluginAvailable && settings.value.sellPricing.autoFromEcoShop

    /**
     * @return 1 if [item] is sellable through EcoShop, else null.
     *
     * Takes the stack rather than its material, so a custom item is offered to
     * EcoShop as itself instead of as the vanilla block it happens to look like.
     *
     * Sell prices are per item, so one unit is one item; the count only matters
     * at payment time.
     */
    fun unitsFor(item: ItemStack): Int? {
        if (!isEnabled()) {
            return null
        }
        val shopItem = item.shopItem ?: return null
        return if (shopItem.sellPrice != null) 1 else null
    }

    /**
     * Pays [player] for [units] of [item] at EcoShop's price.
     *
     * Uses getSellStatus rather than getCurrentSellStatus: the latter checks the
     * player's own inventory, which is irrelevant to a minion's storage.
     *
     * @return Whether the sale went through.
     */
    fun sell(player: Player, item: ItemStack, units: Int, multiplier: Double): Boolean {
        if (!isEnabled() || units <= 0) {
            return false
        }
        val shopItem = item.shopItem ?: return false
        val price = shopItem.sellPrice ?: return false

        if (shopItem.getSellStatus(player, units) != SellStatus.ALLOW) {
            return false
        }

        val dynamic = shopItem.getEffectiveSellMultiplier(player)
        price.giveTo(player, units.toDouble() * dynamic * multiplier)
        shopItem.recordSell(player, units)
        return true
    }
}
