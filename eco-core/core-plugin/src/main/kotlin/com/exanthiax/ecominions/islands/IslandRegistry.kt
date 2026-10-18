package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.IslandsManager
import com.exanthiax.ecominions.api.island.MinionIsland
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The registered [IslandProvider]s and the single lookup point for island checks.
 *
 * Safe to read from any region thread. A provider that throws is treated as having no
 * answer and is reported through [warn] once per provider id.
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
        registered.firstNotNullOfOrNull { provider -> guarded(provider) { it.islandAt(location) } }

    /** @return Every island [uuid] owns or belongs to, across all providers. */
    fun islandsOf(uuid: UUID): Collection<MinionIsland> =
        registered.flatMap { provider -> guarded(provider) { it.islandsOf(uuid) } ?: emptyList() }

    /** @return Whether no provider is registered. */
    fun isEmpty(): Boolean = registered.isEmpty()

    private fun <T> guarded(provider: IslandProvider, query: (IslandProvider) -> T): T? =
        try {
            query(provider)
        } catch (exception: Exception) {
            if (warnedIds.add(provider.id)) {
                warn("Island provider '${provider.id}' failed and will be ignored: ${exception.message}")
            }
            null
        }
}
