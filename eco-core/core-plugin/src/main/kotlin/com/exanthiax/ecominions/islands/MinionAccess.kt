package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Decides who may place, open and pick up minions, owner first and island privileges second.
 *
 * Without an island at the minion, or with island privileges off, only the owner and admins get in.
 */
class MinionAccess(
    private val islands: IslandRegistry,
    private val settings: SettingsHolder<MinionSettings>
) {
    fun canPlace(player: Player, location: Location): Boolean = canPlace(player) { islands.islandAt(location) }

    /** As [canPlace], asking [island] for the island at the spot only when the answer depends on it. */
    fun canPlace(player: Player, island: () -> MinionIsland?): Boolean {
        if (isAdmin(player) || !settings.value.islandPrivileges) {
            return true
        }
        return island()?.isAllowed(player, IslandPrivilege.PLACE) ?: true
    }

    fun canUse(player: Player, minion: Minion): Boolean = canAccess(player, minion, IslandPrivilege.USE)

    fun canPickUp(player: Player, minion: Minion): Boolean = canAccess(player, minion, IslandPrivilege.PICKUP)

    /** Whether [privilege] is the only thing keeping [player] from [minion], so the island message fits. */
    fun deniedByIsland(player: Player, minion: Minion, privilege: IslandPrivilege): Boolean {
        if (isAdmin(player) || ownsOrUnowned(player, minion) || !settings.value.islandPrivileges) {
            return false
        }
        val island = islands.islandAt(minion.location) ?: return false
        return !island.isAllowed(player, privilege)
    }

    private fun canAccess(player: Player, minion: Minion, privilege: IslandPrivilege): Boolean {
        if (isAdmin(player) || ownsOrUnowned(player, minion)) {
            return true
        }
        if (!settings.value.islandPrivileges) {
            return false
        }
        return islands.islandAt(minion.location)?.isAllowed(player, privilege) ?: false
    }

    private fun ownsOrUnowned(player: Player, minion: Minion): Boolean =
        minion.state.owner.let { it == null || it == player.uniqueId }

    private fun isAdmin(player: Player): Boolean = player.hasPermission(ADMIN_PERMISSION)

    private companion object {
        const val ADMIN_PERMISSION = "ecominions.admin"
    }
}
