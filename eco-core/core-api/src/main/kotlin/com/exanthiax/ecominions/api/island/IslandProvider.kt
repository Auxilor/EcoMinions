package com.exanthiax.ecominions.api.island

import org.bukkit.Location
import java.util.UUID

/**
 * A source of islands, backed by one island plugin.
 *
 * Calls come from region threads, so a provider and the islands it returns must be thread-safe.
 * [MinionIsland.members] may be called from any thread, including the global region, as
 * activity checks reuse an island resolved earlier.
 *
 * Register the provider in your plugin's `onEnable` and unregister it on disable. Minions in
 * chunks that are already loaded are indexed on their island when they next register, which
 * is when their chunk next loads.
 */
interface IslandProvider {
    /** The provider's id, unique among registered providers. */
    val id: String

    /** @return The island that contains [location], or null if there is none. */
    fun islandAt(location: Location): MinionIsland?

    /** @return Every island [uuid] owns or belongs to. */
    fun islandsOf(uuid: UUID): Collection<MinionIsland>
}
