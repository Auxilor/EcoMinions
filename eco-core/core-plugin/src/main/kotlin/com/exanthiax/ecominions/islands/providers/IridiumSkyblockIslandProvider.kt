package com.exanthiax.ecominions.islands.providers

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.MinionIsland
import com.iridium.iridiumskyblock.api.IridiumSkyblockAPI
import com.iridium.iridiumskyblock.dependencies.iridiumcore.Item
import com.iridium.iridiumskyblock.dependencies.iridiumteams.Permission
import com.iridium.iridiumskyblock.dependencies.xseries.XMaterial
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.iridium.iridiumskyblock.database.Island as IridiumIsland

/** Island provider backed by IridiumSkyblock. */
class IridiumSkyblockIslandProvider(private val warn: (String) -> Unit) : IslandProvider {
    override val id = PLUGIN_NAME

    private val permissions = ConcurrentHashMap<IslandPrivilege, Permission>()

    /** Registers the three EcoMinions permissions with IridiumSkyblock, reusing any that are already registered. */
    fun register() {
        val api = IridiumSkyblockAPI.getInstance()
        IslandPrivilege.entries.forEach { privilege ->
            val key = permissionKey(privilege)
            val permission = api.getPermissions(key).orElseGet {
                buildPermission(privilege).also { api.addPermission(it, key) }
            }
            permissions[privilege] = permission
        }
    }

    override fun islandAt(location: Location): MinionIsland? =
        IridiumSkyblockAPI.getInstance().getIslandViaLocation(location).map(::wrap).orElse(null)

    override fun islandsOf(uuid: UUID): Collection<MinionIsland> {
        return IridiumSkyblockAPI.getInstance().getUser(Bukkit.getOfflinePlayer(uuid)).island
            .map { listOf(wrap(it)) }
            .orElse(emptyList())
    }

    private fun wrap(island: IridiumIsland): MinionIsland = IridiumMinionIsland(island, permissions, warn)

    private class IridiumMinionIsland(
        private val island: IridiumIsland,
        private val permissions: Map<IslandPrivilege, Permission>,
        private val warn: (String) -> Unit
    ) : MinionIsland {
        override val provider = PLUGIN_NAME
        override val id: String = island.id.toString()
        override val owner: UUID? = island.owner.map { it.uuid }.orElse(null)

        override fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean {
            val permission = permissions[privilege]
            if (permission == null) {
                if (reportedMissing.add(permissionKey(privilege))) {
                    warn("IridiumSkyblock permission ${permissionKey(privilege)} is not registered, denying it")
                }
                return false
            }
            val api = IridiumSkyblockAPI.getInstance()
            return api.getIslandPermission(island, api.getUser(player), permission, permissionKey(privilege))
        }

        override fun isMember(uuid: UUID): Boolean = uuid in members()

        override fun members(): Collection<UUID> = (island.members.map { it.uuid } + listOfNotNull(owner)).distinct()
    }

    companion object {
        const val PLUGIN_NAME = "IridiumSkyblock"

        private const val MEMBER_RANK = 1
        private const val OWNER_ONLY_RANK = Int.MAX_VALUE
        private const val PERMISSION_PAGE = 1

        private val reportedMissing = ConcurrentHashMap.newKeySet<String>()

        private fun permissionKey(privilege: IslandPrivilege) = when (privilege) {
            IslandPrivilege.PLACE -> "ecominionsPlace"
            IslandPrivilege.USE -> "ecominionsUse"
            IslandPrivilege.PICKUP -> "ecominionsPickup"
        }

        private fun buildPermission(privilege: IslandPrivilege): Permission {
            val spec = when (privilege) {
                IslandPrivilege.PLACE -> PermissionSpec(
                    XMaterial.ARMOR_STAND, 37, "&9Place Minions", "&7Grant the ability to place minions.", MEMBER_RANK
                )

                IslandPrivilege.USE -> PermissionSpec(
                    XMaterial.CHEST, 38, "&9Use Minions", "&7Grant the ability to open and use minions.", MEMBER_RANK
                )

                IslandPrivilege.PICKUP -> PermissionSpec(
                    XMaterial.HOPPER, 39, "&9Pick Up Minions", "&7Grant the ability to pick up minions.", OWNER_ONLY_RANK
                )
            }
            val lore = listOf(spec.description, "", "&9&lPermission", "%permission%")
            return Permission(Item(spec.material, spec.slot, 1, spec.displayName, lore), PERMISSION_PAGE, spec.defaultRank)
        }
    }

    private data class PermissionSpec(
        val material: XMaterial,
        val slot: Int,
        val displayName: String,
        val description: String,
        val defaultRank: Int
    )
}
