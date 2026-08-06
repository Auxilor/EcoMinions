package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/** How many minions a player may place. */
class PlacementLimits(
    private val settings: SettingsHolder<MinionSettings>
) {
    fun limitFor(player: OfflinePlayer): Int {
        val current = settings.value
        if (!current.limitsEnabled) {
            return Int.MAX_VALUE
        }
        if (!current.permissionOverrides || player !is Player) {
            return current.defaultLimit
        }
        return highestPermissionLimit(player, current.defaultLimit)
    }

    /** Permission overrides raise the limit; the most generous one wins. */
    private fun highestPermissionLimit(player: Player, default: Int): Int {
        var best = default
        for (info in player.effectivePermissions) {
            if (!info.value || !info.permission.startsWith(LIMIT_PREFIX)) {
                continue
            }
            val limit = info.permission.removePrefix(LIMIT_PREFIX).toIntOrNull() ?: continue
            if (limit > best) {
                best = limit
            }
        }
        return best
    }

    private companion object {
        const val LIMIT_PREFIX = "ecominions.limit."
    }
}
