package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.minions.service.Minion

/**
 * Keeps the [IslandMinionIndex] and [IslandMemberCache] in step as minions register and retire.
 *
 * [register] runs on the minion's own region, so the island is resolved there once and reused.
 * A minion no provider claims leaves the index, unless a provider failed or none is registered,
 * where the answer is unknown and its entry is kept.
 */
class IslandMinionTracker(
    private val islands: IslandRegistry,
    private val index: IslandMinionIndex,
    private val members: IslandMemberCache
) {
    fun register(minion: Minion) {
        when (val lookup = islands.lookup(minion.location)) {
            is IslandLookup.Found -> {
                index.record(lookup.island, minion.uuid)
                members.remember(minion.uuid, lookup.island)
            }

            IslandLookup.None -> {
                if (!islands.isEmpty()) {
                    index.remove(minion.uuid)
                }
                members.remember(minion.uuid, null)
            }

            IslandLookup.Failed -> members.remember(minion.uuid, null)
        }
    }

    fun retire(minion: Minion) {
        index.remove(minion.uuid)
        members.forget(minion.uuid)
    }
}
