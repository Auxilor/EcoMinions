package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.model.ExperienceMode
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.OverLimitMode
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Material

/** Reads `config.yml` into [MinionSettings], re-run on every reload. */
class MinionSettingsLoader(
    private val config: Config,
    private val warn: (String) -> Unit = {}
) {
    fun load(): MinionSettings = MinionSettings(
        hologramOffset = config.getDouble("minion.hologram-offset"),
        hologramRefreshTicks = config.getInt("minion.hologram-refresh-ticks").coerceAtLeast(1),
        invulnerable = config.getBool("minion.invulnerable"),
        showRegenMarker = config.getBool("minion.show-regen-marker"),
        regenMarkerFallback = Material.matchMaterial(
            config.getString("minion.regen-marker-fallback-block").uppercase()
        ) ?: Material.AIR,
        clearSeededOnPickup = config.getStringOrNull("minion.seeded-blocks-on-pickup")
            ?.equals("clear", ignoreCase = true) == true,
        hardSlotCap = config.getInt("minion.hard-slot-cap").coerceAtLeast(1),
        offlineEnabled = config.getBool("unloaded-calculation.enabled"),
        offlineEfficiency = config.getDouble("unloaded-calculation.offline-efficiency"),
        limitsEnabled = config.getBool("limits.per-player.enabled"),
        defaultLimit = config.getInt("limits.per-player.default"),
        permissionOverrides = config.getBool("limits.per-player.permission-overrides"),
        sellPricing = SellPricingSettings(
            autoFromEcoShop = config.getBool("sell-pricing.auto-from-ecoshop"),
            multiplier = config.getDouble("sell-pricing.sell-price-multiplier")
        ),
        experienceMode = ExperienceMode.entries.firstOrNull {
            it.name.equals(config.getStringOrNull("minion.xp.mode")?.trim(), ignoreCase = true)
        } ?: ExperienceMode.STORE,
        islandPrivileges = config.getBoolOrNull("islands.privileges") ?: true,
        activeWhileMemberOnline = config.getBoolOrNull("islands.active-while-member-online") ?: false,
        islandLimitsEnabled = config.getBoolOrNull("limits.per-island.enabled") ?: false,
        defaultIslandLimit = config.getIntOrNull("limits.per-island.default") ?: 10,
        islandPermissionOverrides = config.getBoolOrNull("limits.per-island.permission-overrides") ?: true,
        overLimit = readOverLimit(),
        typeLimitsEnabled = config.getBoolOrNull("limits.per-type.enabled") ?: false,
        typeLimitOverrides = config.getBoolOrNull("limits.per-type.permission-overrides") ?: true,
        typeLimits = readTypeLimits()
    )

    private fun readOverLimit(): OverLimitMode {
        val value = config.getStringOrNull("limits.over-limit")?.trim() ?: return OverLimitMode.PREVENT
        return OverLimitMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OverLimitMode.PREVENT.also {
            warn("Unknown limits.over-limit '$value', using prevent. Use prevent or inactive.")
        }
    }

    private fun readTypeLimits(): Map<MinionType, Int> {
        val section = config.getSubsectionOrNull("limits.per-type.types") ?: return emptyMap()
        return section.getKeys(false).mapNotNull { key ->
            MinionType.entries.firstOrNull { it.id == key.lowercase() }?.let { it to section.getInt(key).coerceAtLeast(0) }
        }.toMap()
    }
}
