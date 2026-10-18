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
    /** Whether a minion clears the blocks it placed when it is picked up or removed. */
    val clearSeededOnPickup: Boolean,
    /** Upper bound on any type's declared storage slots. A typo guard, not balance. */
    val hardSlotCap: Int,
    val offlineEnabled: Boolean,
    val offlineEfficiency: Double,
    val limitsEnabled: Boolean,
    val defaultLimit: Int,
    val permissionOverrides: Boolean,
    val sellPricing: SellPricingSettings,
    val experienceMode: ExperienceMode = ExperienceMode.STORE,
    /** Whether island ranks decide who may place, use and pick up minions. */
    val islandPrivileges: Boolean = true,
    /** Whether a minion on an island keeps working while any island member is online. */
    val activeWhileMemberOnline: Boolean = false,
    val islandLimitsEnabled: Boolean = false,
    val defaultIslandLimit: Int = 10,
    val islandPermissionOverrides: Boolean = true
)

/** Where the XP a minion earns goes. */
enum class ExperienceMode {
    /** Kept on the minion until the owner claims it from the menu. */
    STORE,

    /** Dropped as an orb at the minion, as before stored XP existed. */
    ORB
}
