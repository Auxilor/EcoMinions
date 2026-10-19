package com.exanthiax.ecominions.islands.providers

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.MinionIsland
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import world.bentobox.bentobox.BentoBox
import world.bentobox.bentobox.api.flags.Flag
import world.bentobox.bentobox.api.user.User
import world.bentobox.bentobox.managers.RanksManager
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import world.bentobox.bentobox.database.objects.Island as BentoBoxIsland

/** Island provider backed by BentoBox. */
class BentoBoxIslandProvider(private val warn: (String) -> Unit) : IslandProvider {
    override val id = PLUGIN_NAME

    private val flags = ConcurrentHashMap<IslandPrivilege, Flag>()

    /** Registers the three EcoMinions flags with BentoBox, reusing any that are already registered. */
    fun register() {
        val flagsManager = BentoBox.getInstance().flagsManager
        IslandPrivilege.entries.forEach { privilege ->
            val flagId = flagId(privilege)
            val flag = flagsManager.getFlag(flagId).orElseGet {
                buildFlag(privilege, flagId).also(flagsManager::registerFlag)
            }
            flags[privilege] = flag
        }
    }

    override fun islandAt(location: Location): MinionIsland? =
        BentoBox.getInstance().islands.getProtectedIslandAt(location).map(::wrap).orElse(null)

    override fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        BentoBox.getInstance().islands.getIslands(uuid).map(::wrap)

    private fun wrap(island: BentoBoxIsland): MinionIsland = BentoBoxMinionIsland(island, flags, warn)

    private class BentoBoxMinionIsland(
        private val island: BentoBoxIsland,
        private val flags: Map<IslandPrivilege, Flag>,
        private val warn: (String) -> Unit
    ) : MinionIsland {
        override val provider = PLUGIN_NAME
        override val id: String = island.uniqueId
        override val owner: UUID? = island.owner

        override fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean {
            val flag = flags[privilege]
            if (flag == null) {
                if (reportedMissing.add(flagId(privilege))) {
                    warn("BentoBox flag ${flagId(privilege)} is not registered, denying it")
                }
                return false
            }
            return island.isAllowed(User.getInstance(player), flag)
        }

        override fun isMember(uuid: UUID): Boolean = owner == uuid || uuid in island.memberSet

        override fun members(): Collection<UUID> = island.memberSet + listOfNotNull(owner)
    }

    companion object {
        const val PLUGIN_NAME = "BentoBox"

        private val reportedMissing = ConcurrentHashMap.newKeySet<String>()

        private fun flagId(privilege: IslandPrivilege) = "ECOMINIONS_${privilege.name}"

        private fun buildFlag(privilege: IslandPrivilege, flagId: String): Flag {
            val (icon, rank, displayName, description) = when (privilege) {
                IslandPrivilege.PLACE -> FlagSpec(
                    Material.ARMOR_STAND, RanksManager.MEMBER_RANK, "Place minions", "Allow placing minions"
                )

                IslandPrivilege.USE -> FlagSpec(
                    Material.CHEST, RanksManager.MEMBER_RANK, "Use minions", "Allow opening and using minions"
                )

                IslandPrivilege.PICKUP -> FlagSpec(
                    Material.HOPPER, RanksManager.OWNER_RANK, "Pick up minions", "Allow picking up minions"
                )
            }
            return Flag.Builder(flagId, icon)
                .type(Flag.Type.PROTECTION)
                .defaultRank(rank)
                .build()
                .also {
                    it.setTranslatedName(Locale.US, displayName)
                    it.setTranslatedDescription(Locale.US, description)
                }
        }
    }

    private data class FlagSpec(val icon: Material, val rank: Int, val displayName: String, val description: String)
}
