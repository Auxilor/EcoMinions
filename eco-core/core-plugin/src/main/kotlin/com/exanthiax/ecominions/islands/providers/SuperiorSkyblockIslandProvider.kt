package com.exanthiax.ecominions.islands.providers

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI
import com.bgsoftware.superiorskyblock.api.events.IslandCreateEvent
import com.bgsoftware.superiorskyblock.api.island.Island
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer
import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.MinionIsland
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege as SuperiorPrivilege

/** Island provider backed by SuperiorSkyblock2. */
class SuperiorSkyblockIslandProvider : IslandProvider {
    override val id = PLUGIN_NAME

    override fun islandAt(location: Location): MinionIsland? =
        SuperiorSkyblockAPI.getIslandAt(location)?.let(::wrap)

    override fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        SuperiorSkyblockAPI.getPlayer(uuid)?.island?.let { listOf(wrap(it)) }.orEmpty()

    private fun wrap(island: Island): MinionIsland {
        applyDefaultsOnce(island)
        return SuperiorSkyblockMinionIsland(island)
    }

    private class SuperiorSkyblockMinionIsland(private val island: Island) : MinionIsland {
        override val provider = PLUGIN_NAME
        override val id: String = island.uniqueId.toString()
        override val owner: UUID? = island.owner?.uniqueId

        override fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean {
            val name = privilegeName(privilege)
            val superiorPrivilege = SuperiorPrivilege.getByName(name)
            if (superiorPrivilege == null) {
                if (reportedMissing.add(name)) {
                    Bukkit.getLogger().warning("SuperiorSkyblock2 privilege $name is not registered, denying it")
                }
                return false
            }
            return island.hasPermission(SuperiorSkyblockAPI.getPlayer(player), superiorPrivilege)
        }

        override fun isMember(uuid: UUID): Boolean =
            owner == uuid || SuperiorSkyblockAPI.getPlayer(uuid)?.let(island::isMember) == true

        override fun members(): Collection<UUID> =
            island.getIslandMembers(true).map(SuperiorPlayer::getUniqueId)
    }

    private class IslandCreateListener : Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onIslandCreate(event: IslandCreateEvent) {
            applyDefaultsOnce(event.island)
        }
    }

    companion object {
        const val PLUGIN_NAME = "SuperiorSkyblock2"

        private val defaultsApplied = ConcurrentHashMap.newKeySet<UUID>()
        private val reportedMissing = ConcurrentHashMap.newKeySet<String>()

        private fun privilegeName(privilege: IslandPrivilege) = "ECOMINIONS_${privilege.name}"

        /** Registers the three EcoMinions privileges with SuperiorSkyblock2 unless already registered. */
        fun registerPrivileges() {
            IslandPrivilege.entries.map(::privilegeName).forEach { name ->
                if (SuperiorPrivilege.getByName(name) == null) {
                    SuperiorPrivilege.register(name)
                }
            }
        }

        /** Listens for new islands so they get their default privileges. Call after [registerPrivileges]. */
        fun listenForNewIslands(plugin: Plugin) {
            Bukkit.getPluginManager().registerEvents(IslandCreateListener(), plugin)
        }

        private fun applyDefaultsOnce(island: Island) {
            if (defaultsApplied.add(island.uniqueId)) {
                applyDefaults(island)
            }
        }

        /**
         * Gives [island] the default EcoMinions privileges: place and use for the default
         * member role and above. Pickup stays owner only, which is what SuperiorSkyblock2
         * assigns to a privilege nothing else covers.
         *
         * A privilege counts as unset only when the island has no explicit role for it in
         * its role permissions and the required role is still the last (owner) role, meaning
         * no SuperiorSkyblock2 config default applies either. Anything else is an admin
         * choice and is left alone.
         */
        private fun applyDefaults(island: Island) {
            val memberRole = SuperiorSkyblockAPI.getRoles().defaultRole
            listOf(IslandPrivilege.PLACE, IslandPrivilege.USE).forEach { privilege ->
                SuperiorPrivilege.getByName(privilegeName(privilege))?.takeIf { superiorPrivilege ->
                    !island.rolePermissions.containsKey(superiorPrivilege) &&
                        island.getRequiredPlayerRole(superiorPrivilege).isLastRole
                }?.let { island.setPermission(memberRole, it) }
            }
        }
    }
}
