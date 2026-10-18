package com.exanthiax.ecominions.api.island

import org.bukkit.Location
import java.util.UUID

/** A source of islands, backed by one island plugin. */
interface IslandProvider {
    /** The provider's id, unique among registered providers. */
    val id: String

    /** @return The island that contains [location], or null if there is none. */
    fun islandAt(location: Location): MinionIsland?

    /** @return Every island [uuid] owns or belongs to. */
    fun islandsOf(uuid: UUID): Collection<MinionIsland>
}
