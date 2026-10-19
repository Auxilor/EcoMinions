package com.exanthiax.ecominions.api.island

import org.bukkit.entity.Player
import java.util.UUID

/** An island, claim or similar region owned by an island plugin. */
interface MinionIsland {
    /** The id of the [IslandProvider] this island belongs to. */
    val provider: String

    /** The island's id, unique within its [provider]. */
    val id: String

    /** The island's owner, or null if it has none. */
    val owner: UUID?

    /** @return Whether [player] holds [privilege] on this island. */
    fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean

    /** @return Whether [uuid] is the owner or a member of this island. */
    fun isMember(uuid: UUID): Boolean = uuid in members()

    /** @return Everyone on this island, the owner included. */
    fun members(): Collection<UUID>
}
