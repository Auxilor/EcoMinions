package com.exanthiax.ecominions.minions.service

import java.util.UUID

/**
 * Every minion each player has placed, loaded or not, kept in the player's eco data.
 *
 * Live minions only exist while their chunk is loaded, so counting those alone
 * would let a player pass their limit by placing while the others sit unloaded.
 * Entries are `uuid:type`, so per-type counts work without loading anything.
 *
 * Storage is injected so the bookkeeping can be tested without a server.
 */
class PlacedMinionRegistry(
    private val read: (UUID) -> List<String>,
    private val write: (UUID, List<String>) -> Unit
) {
    fun count(owner: UUID): Int = read(owner).size

    fun count(owner: UUID, typeId: String): Int = read(owner).count { it.substringAfter(SEPARATOR) == typeId }

    /** Records [minion] for [owner]; a minion already recorded is left as it is. */
    @Synchronized
    fun add(owner: UUID, minion: UUID, typeId: String) {
        val entries = read(owner)
        if (entries.none { it.startsWith("$minion$SEPARATOR") }) {
            write(owner, entries + "$minion$SEPARATOR$typeId")
        }
    }

    @Synchronized
    fun remove(owner: UUID, minion: UUID) {
        val entries = read(owner)
        val kept = entries.filterNot { it.startsWith("$minion$SEPARATOR") }
        if (kept.size != entries.size) {
            write(owner, kept)
        }
    }

    /** Forgets every minion [owner] placed; those still in the world record themselves again when they load. */
    @Synchronized
    fun clear(owner: UUID) {
        write(owner, emptyList())
    }

    private companion object {
        const val SEPARATOR = ":"
    }
}
