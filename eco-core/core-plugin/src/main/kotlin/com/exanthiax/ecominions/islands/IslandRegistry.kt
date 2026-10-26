package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.IslandsManager
import com.exanthiax.ecominions.api.island.MinionIsland
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The registered [IslandProvider]s and the single lookup point for island checks.
 *
 * Safe to read from any region thread. A provider that throws, or fails to link against its
 * island plugin, is treated as having no answer and is reported through [warn] once per
 * provider id. Islands it returns are wrapped the same way: a failing island denies every
 * privilege and has no members.
 */
class IslandRegistry(private val warn: (String) -> Unit) : IslandsManager {
    private val registered = CopyOnWriteArrayList<IslandProvider>()
    private val warnedIds = ConcurrentHashMap.newKeySet<String>()

    @Synchronized
    override fun register(provider: IslandProvider) {
        val existingIndex = registered.indexOfFirst { it.id == provider.id }
        if (existingIndex >= 0) {
            registered[existingIndex] = provider
        } else {
            registered.add(provider)
        }
        warnedIds.remove(provider.id)
    }

    @Synchronized
    override fun unregister(id: String) {
        registered.removeIf { it.id == id }
        warnedIds.remove(id)
    }

    override fun providers(): List<IslandProvider> = registered.toList()

    override fun islandAt(location: Location): MinionIsland? = (lookup(location) as? IslandLookup.Found)?.island

    /** As [islandAt], telling "no provider claims [location]" apart from "a provider failed". */
    fun lookup(location: Location): IslandLookup {
        var failed = false
        for (provider in registered) {
            attempt(provider.id) { provider.islandAt(location)?.let { GuardedIsland(it, provider.id) } }
                .onFailure { failed = true }
                .getOrNull()
                ?.let { return IslandLookup.Found(it) }
        }
        return if (failed) IslandLookup.Failed else IslandLookup.None
    }

    /** @return Every island [uuid] owns or belongs to, across all providers. */
    fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        registered.flatMap { provider ->
            guarded(provider.id) { provider.islandsOf(uuid).map { GuardedIsland(it, provider.id) } }.orEmpty()
        }

    /** @return Whether no provider is registered. */
    fun isEmpty(): Boolean = registered.isEmpty()

    private fun <T> guarded(providerId: String, query: () -> T): T? = attempt(providerId, query).getOrNull()

    private fun <T> attempt(providerId: String, query: () -> T): Result<T> =
        try {
            Result.success(query())
        } catch (exception: Exception) {
            reportFailure(providerId, exception)
            Result.failure(exception)
        } catch (error: LinkageError) {
            reportFailure(providerId, error)
            Result.failure(error)
        }

    private fun reportFailure(providerId: String, failure: Throwable) {
        if (warnedIds.add(providerId)) {
            warn("Island provider '$providerId' failed and will be ignored: ${failure.message ?: failure.javaClass.name}")
        }
    }

    /** An island whose calls fail closed: denied, no members. Its identity is read once, up front. */
    private inner class GuardedIsland(private val island: MinionIsland, private val providerId: String) : MinionIsland {
        override val provider: String = island.provider
        override val id: String = island.id
        override val owner: UUID? = island.owner

        override fun isAllowed(player: Player, privilege: IslandPrivilege): Boolean =
            guarded(providerId) { island.isAllowed(player, privilege) } ?: false

        override fun isMember(uuid: UUID): Boolean = guarded(providerId) { island.isMember(uuid) } ?: false

        override fun members(): Collection<UUID> = guarded(providerId) { island.members() }.orEmpty()
    }
}

/** The outcome of [IslandRegistry.lookup]. */
sealed interface IslandLookup {
    data class Found(val island: MinionIsland) : IslandLookup

    /** Every provider answered and none claims the location. */
    data object None : IslandLookup

    /** No provider claims the location, and at least one failed, so the answer is unknown. */
    data object Failed : IslandLookup
}
