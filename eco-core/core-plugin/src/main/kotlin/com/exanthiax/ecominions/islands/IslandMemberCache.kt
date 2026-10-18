package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.minions.service.Minion
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The island members of each minion, looked up through the [IslandRegistry] at most once per
 * [TTL_MILLIS] so per-tick activity checks do not query island providers.
 */
class IslandMemberCache(
    private val islands: IslandRegistry,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val entries = ConcurrentHashMap<UUID, Entry>()

    fun membersOf(minion: Minion): Collection<UUID> {
        val now = clock()
        entries[minion.uuid]?.takeIf { now - it.lookedUpAt <= TTL_MILLIS }?.let { return it.members }
        val members = islands.islandAt(minion.location)?.members().orEmpty()
        entries[minion.uuid] = Entry(members, now)
        return members
    }

    fun forget(minion: UUID) {
        entries.remove(minion)
    }

    private class Entry(val members: Collection<UUID>, val lookedUpAt: Long)

    private companion object {
        const val TTL_MILLIS = 5000L
    }
}
