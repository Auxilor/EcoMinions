package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/** How many minions a player may place. Admins bypass the limit. */
class PlacementLimits(
    private val settings: SettingsHolder<MinionSettings>
) {
    fun limitFor(player: OfflinePlayer): Int {
        val current = settings.value
        if (!current.limitsEnabled || (player is Player && player.hasPermission(ADMIN_PERMISSION))) {
            return UNLIMITED
        }
        if (!current.permissionOverrides || player !is Player) {
            return current.defaultLimit
        }
        return highestPermissionLimit(player, LIMIT_PREFIX, current.defaultLimit)
    }

    companion object {
        const val UNLIMITED = Int.MAX_VALUE
        private const val LIMIT_PREFIX = "ecominions.limit."
        private const val ADMIN_PERMISSION = "ecominions.admin"
    }
}
