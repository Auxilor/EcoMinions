package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.MinionIsland
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The island each minion stands on and its members, so per-tick activity checks never look an
 * island up by location.
 *
 * The island is resolved once, on the minion's own region, when the minion registers (minions
 * never move). Members are asked of that island at most once per [TTL_MILLIS], from whichever
 * thread asks, so the entity is never touched off its region.
 */
class IslandMemberCache(
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val islands = ConcurrentHashMap<UUID, MinionIsland>()
    private val entries = ConcurrentHashMap<UUID, Entry>()

    /** Records [island] as the one [minion] stands on, or none, dropping any cached members. */
    fun remember(minion: UUID, island: MinionIsland?) {
        if (island == null) {
            islands.remove(minion)
        } else {
            islands[minion] = island
        }
        entries.remove(minion)
    }

    fun membersOf(minion: UUID): Collection<UUID> {
        val island = islands[minion] ?: return emptyList()
        val now = clock()
        entries[minion]?.takeIf { now - it.lookedUpAt <= TTL_MILLIS }?.let { return it.members }
        val members = island.members()
        entries[minion] = Entry(members, now)
        return members
    }

    fun forget(minion: UUID) {
        islands.remove(minion)
        entries.remove(minion)
    }

    private class Entry(val members: Collection<UUID>, val lookedUpAt: Long)

    private companion object {
        const val TTL_MILLIS = 5000L
    }
}
