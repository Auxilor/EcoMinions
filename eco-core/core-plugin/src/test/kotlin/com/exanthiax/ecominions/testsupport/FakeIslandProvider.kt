package com.exanthiax.ecominions.testsupport

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.MinionIsland
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

/** An island whose membership and privileges are plain mutable collections. */
class FakeIsland(
    override val provider: String,
    override val id: String,
    override val owner: UUID?,
    val members: MutableSet<UUID> = mutableSetOf(),
    val allowed: MutableMap<UUID, MutableSet<IslandPrivilege>> = mutableMapOf()
) : MinionIsland {
    override fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean =
        allowed[player.uniqueId]?.contains(privilege) == true

    override fun isMember(uuid: UUID): Boolean = uuid == owner || uuid in members

    override fun members(): Collection<UUID> = (members + listOfNotNull(owner)).toSet()
}

/** A provider that answers from islands claimed on block positions. */
class FakeIslandProvider(override val id: String) : IslandProvider {
    private val claims = HashMap<BlockPosition, FakeIsland>()
    private val islands = LinkedHashSet<FakeIsland>()

    /** Makes [island] the answer for the block of each of [locations]. */
    fun claim(island: FakeIsland, vararg locations: Location) {
        islands += island
        locations.forEach { claims[BlockPosition.of(it)] = island }
    }

    override fun islandAt(location: Location): MinionIsland? = claims[BlockPosition.of(location)]

    override fun islandsOf(uuid: UUID): Collection<MinionIsland> = islands.filter { it.isMember(uuid) }

    private data class BlockPosition(val world: String?, val x: Int, val y: Int, val z: Int) {
        companion object {
            fun of(location: Location) =
                BlockPosition(location.world?.name, location.blockX, location.blockY, location.blockZ)
        }
    }
}
