package com.exanthiax.ecominions.api.island

import org.bukkit.Location

/** The registered [IslandProvider]s. When several claim a location, the first registered wins. */
interface IslandsManager {
    /** Adds [provider], replacing any provider with the same id in place. */
    fun register(provider: IslandProvider)

    /** Removes the provider with [id]. Does nothing if there is none. */
    fun unregister(id: String)

    /** @return The registered providers, in registration order. */
    fun providers(): List<IslandProvider>

    /** @return The island at [location] from the first provider that has one, or null if there is none. */
    fun islandAt(location: Location): MinionIsland?
}
