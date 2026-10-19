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

    override fun islandAt(location: Location): MinionIsland? =
        registered.firstNotNullOfOrNull { provider ->
            guarded(provider.id) { provider.islandAt(location)?.let { GuardedIsland(it, provider.id) } }
        }

    /** @return Every island [uuid] owns or belongs to, across all providers. */
    fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        registered.flatMap { provider ->
            guarded(provider.id) { provider.islandsOf(uuid).map { GuardedIsland(it, provider.id) } }.orEmpty()
        }

    /** @return Whether no provider is registered. */
    fun isEmpty(): Boolean = registered.isEmpty()

    private fun <T> guarded(providerId: String, query: () -> T): T? =
        try {
            query()
        } catch (exception: Exception) {
            reportFailure(providerId, exception)
            null
        } catch (error: LinkageError) {
            reportFailure(providerId, error)
            null
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
