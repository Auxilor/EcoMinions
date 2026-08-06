package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.model.MinionSettings
import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Material

/** Reads `config.yml` into [MinionSettings], re-run on every reload. */
class MinionSettingsLoader(
    private val config: Config
) {
    fun load(): MinionSettings = MinionSettings(
        hologramOffset = config.getDouble("minion.hologram-offset"),
        hologramRefreshTicks = config.getInt("minion.hologram-refresh-ticks").coerceAtLeast(1),
        invulnerable = config.getBool("minion.invulnerable"),
        showRegenMarker = config.getBool("minion.show-regen-marker"),
        regenMarkerFallback = Material.matchMaterial(
            config.getString("minion.regen-marker-fallback-block").uppercase()
        ) ?: Material.AIR,
        autoSellIntervalTicks = config.getInt("minion.auto_sell-interval-ticks").coerceAtLeast(1),
        offlineEnabled = config.getBool("unloaded-calculation.enabled"),
        offlineEfficiency = config.getDouble("unloaded-calculation.offline-efficiency"),
        limitsEnabled = config.getBool("limits.per-player.enabled"),
        defaultLimit = config.getInt("limits.per-player.default"),
        permissionOverrides = config.getBool("limits.per-player.permission-overrides")
    )
}
