package com.exanthiax.ecominions.minions.model

/**
 * How auto-sell decides what a drop is worth.
 *
 * A per-type `sell-prices` entry always wins and is never scaled, so a server
 * owner can pin a price for one material without it drifting when the global
 * multiplier is tuned.
 */
data class SellPricingSettings(
    val autoFromEcoShop: Boolean,
    val multiplier: Double
)
