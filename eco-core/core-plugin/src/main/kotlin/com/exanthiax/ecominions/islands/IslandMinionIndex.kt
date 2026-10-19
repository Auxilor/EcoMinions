package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.MinionIsland
import java.util.UUID

/**
 * Every minion placed on each island, loaded or not, and the last known minion limit of each island.
 *
 * Live minions only exist while their chunk is loaded, so counting those alone would let an island
 * pass its limit while the others sit unloaded. Minion entries are `provider|islandId|minionUuid`
 * and limit entries are `provider|islandId|limit`, so counting never loads a chunk.
 *
 * Storage is injected so the bookkeeping can be tested without a server.
 */
class IslandMinionIndex(
    private val readMinions: () -> List<String>,
    private val writeMinions: (List<String>) -> Unit,
    private val readLimits: () -> List<String>,
    private val writeLimits: (List<String>) -> Unit
) {
    fun count(island: MinionIsland): Int = readMinions().count { it.startsWith(prefix(island)) }

    /** The minions recorded on [island], loaded or not. */
    fun minionsOn(island: MinionIsland): List<UUID> =
        readMinions().filter { it.startsWith(prefix(island)) }.mapNotNull {
            runCatching { UUID.fromString(it.substringAfterLast(SEPARATOR)) }.getOrNull()
        }

    /**
     * Records [minion] on [island], replacing any entry for it on another island, as when a
     * claim is unclaimed and reclaimed. A minion already recorded only there is left as it is.
     */
    @Synchronized
    fun record(island: MinionIsland, minion: UUID) {
        val entries = readMinions()
        val entry = "${prefix(island)}$minion"
        val (current, others) = entries.partition { it.endsWith("$SEPARATOR$minion") }
        if (current != listOf(entry)) {
            writeMinions(others + entry)
        }
    }

    /** Forgets [minion] on every island. */
    @Synchronized
    fun remove(minion: UUID) {
        val entries = readMinions()
        val kept = entries.filterNot { it.endsWith("$SEPARATOR$minion") }
        if (kept.size != entries.size) {
            writeMinions(kept)
        }
    }

    fun cachedLimit(island: MinionIsland): Int? =
        readLimits().firstOrNull { it.startsWith(prefix(island)) }?.substringAfterLast(SEPARATOR)?.toIntOrNull()

    @Synchronized
    fun cacheLimit(island: MinionIsland, limit: Int) {
        val entries = readLimits()
        val entry = "${prefix(island)}$limit"
        if (entry !in entries || entries.count { it.startsWith(prefix(island)) } > 1) {
            writeLimits(entries.filterNot { it.startsWith(prefix(island)) } + entry)
        }
    }

    private fun prefix(island: MinionIsland): String = "${island.provider}$SEPARATOR${island.id}$SEPARATOR"

    private companion object {
        const val SEPARATOR = "|"
    }
}
