package com.exanthiax.ecominions.upgrades.integration

import com.exanthiax.ecominions.upgrades.model.TierCost
import com.willfp.eco.core.price.ConfiguredPrice
import org.bukkit.entity.Player

/** Adapts eco's price system to the slice's [TierCost] port. */
class ConfiguredPriceCost(
    private val price: ConfiguredPrice
) : TierCost {
    override fun canAfford(player: Player): Boolean = price.canAfford(player)

    override fun pay(player: Player) = price.pay(player)

    override fun display(player: Player): String = price.getDisplay(player)
}
