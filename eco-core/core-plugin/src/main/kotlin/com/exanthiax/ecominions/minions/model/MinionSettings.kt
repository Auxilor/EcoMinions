package com.exanthiax.ecominions.minions.model

import org.bukkit.Material

/** Runtime settings for placed minions, refreshed on reload. */
data class MinionSettings(
    val hologramOffset: Double,
    val hologramRefreshTicks: Int,
    val invulnerable: Boolean,
    val showRegenMarker: Boolean,
    /** Block left behind when a minion is picked up mid-regen. */
    val regenMarkerFallback: Material,
    /** Upper bound on any type's declared storage slots. A typo guard, not balance. */
    val hardSlotCap: Int,
    val offlineEnabled: Boolean,
    val offlineEfficiency: Double,
    val limitsEnabled: Boolean,
    val defaultLimit: Int,
    val permissionOverrides: Boolean,
    val sellPricing: SellPricingSettings
)
