package com.exanthiax.ecominions.islands.providers

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.MinionIsland
import me.angeschossen.lands.api.LandsIntegration
import me.angeschossen.lands.api.events.land.create.LandPostCreateEvent
import me.angeschossen.lands.api.flags.enums.FlagTarget
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory
import me.angeschossen.lands.api.flags.type.RoleFlag
import me.angeschossen.lands.api.land.Area
import me.angeschossen.lands.api.land.Land
import me.angeschossen.lands.api.role.enums.RoleType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Island provider backed by Lands, where a land is an island.
 *
 * Lands only accepts flag registration while it loads, so [create] must run from the plugin's
 * load phase. The returned provider is registered later, once Lands is enabled.
 */
class LandsIslandProvider private constructor(
    private val integration: LandsIntegration,
    private val flags: Map<IslandPrivilege, RoleFlag>,
    private val warn: (String) -> Unit
) : IslandProvider {
    override val id = PLUGIN_NAME

    override fun islandAt(location: Location): MinionIsland? =
        integration.getArea(location)?.let { wrap(it, it.land) }

    override fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        landsOf(uuid).map { wrap(it.defaultArea, it) }

    /** Seeds the EcoMinions flags on lands created after startup. Call once Lands is enabled. */
    fun listenForNewLands(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(NewLandListener(flags), plugin)
    }

    private fun landsOf(uuid: UUID): Collection<Land> =
        integration.getLandPlayer(uuid)?.lands
            ?: integration.lands.filter { it.ownerUID == uuid || it.isTrusted(uuid) }

    private fun wrap(area: Area, land: Land): MinionIsland = LandsMinionIsland(area, land, flags, warn)

    private class LandsMinionIsland(
        private val area: Area,
        private val land: Land,
        private val flags: Map<IslandPrivilege, RoleFlag>,
        private val warn: (String) -> Unit
    ) : MinionIsland {
        override val provider = PLUGIN_NAME
        override val id: String = land.ulid.toString()
        override val owner: UUID? = land.ownerUID

        override fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean {
            val flag = flags[privilege]
            if (flag == null) {
                if (reportedMissing.add(flagName(privilege))) {
                    warn("Lands flag ${flagName(privilege)} is not registered, denying it")
                }
                return false
            }
            return area.hasRoleFlag(player.uniqueId, flag)
        }

        override fun isMember(uuid: UUID): Boolean = owner == uuid || land.isTrusted(uuid)

        override fun members(): Collection<UUID> = (land.trustedPlayers + listOfNotNull(owner)).distinct()
    }

    private class NewLandListener(private val flags: Map<IslandPrivilege, RoleFlag>) : Listener {
        @EventHandler
        fun onLandCreate(event: LandPostCreateEvent) {
            event.land.defaultArea.roles.forEach { role ->
                flags.forEach { (privilege, flag) ->
                    if (spec(privilege).enabledForRole(role.type) && !role.hasFlag(flag)) {
                        role.toggleFlag(flag)
                    }
                }
            }
        }
    }

    private data class FlagSpec(
        val icon: Material,
        val displayName: String,
        val description: String,
        val enabledForRole: (RoleType) -> Boolean
    )

    companion object {
        const val PLUGIN_NAME = "Lands"

        private val reportedMissing = ConcurrentHashMap.newKeySet<String>()

        private fun flagName(privilege: IslandPrivilege) = "ecominions_${privilege.name.lowercase()}"

        private val memberRoleTypes = setOf(RoleType.OWNER, RoleType.NORMAL)

        private fun spec(privilege: IslandPrivilege) = when (privilege) {
            IslandPrivilege.PLACE -> FlagSpec(Material.ARMOR_STAND, "Place Minions", "Allow placing minions") {
                it in memberRoleTypes
            }

            IslandPrivilege.USE -> FlagSpec(Material.CHEST, "Use Minions", "Allow opening and using minions") {
                it in memberRoleTypes
            }

            IslandPrivilege.PICKUP -> FlagSpec(Material.HOPPER, "Pick Up Minions", "Allow picking up minions") {
                it == RoleType.OWNER
            }
        }

        /**
         * Registers the three EcoMinions role flags with Lands and returns the provider that checks them.
         *
         * Flags are switched on for the owner and normal member roles (owner only for pick up), never
         * for visitors. Lands seeds roles of lands that exist at startup from the update predicate, and
         * [listenForNewLands] seeds lands created later. An existing flag state is left alone.
         */
        fun create(plugin: Plugin, warn: (String) -> Unit): LandsIslandProvider {
            val integration = LandsIntegration.of(plugin)
            val flags = IslandPrivilege.entries.associateWith { privilege ->
                val spec = spec(privilege)
                RoleFlag.of(integration, FlagTarget.PLAYER, RoleFlagCategory.ACTION, flagName(privilege))
                    .setDefaultState(false)
                    .setIcon(ItemStack(spec.icon))
                    .setDisplayName(spec.displayName)
                    .setDescription(spec.description)
                    .setUpdatePredicate { role -> spec.enabledForRole(role.type) }
            }
            return LandsIslandProvider(integration, flags, warn)
        }
    }
}
