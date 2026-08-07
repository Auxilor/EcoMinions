package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.minion.MinionsManager
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionType
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.OfflinePlayer
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.exanthiax.ecominions.api.minion.MinionType as ApiMinionType

/**
 * Owns the set of live minions: what exists, who owns it, and how stands in the
 * world become minions again after a restart.
 */
class MinionService(
    private val types: ConfigDrivenRegistry<MinionType>,
    private val factory: MinionFactory,
    private val repository: MinionStateRepository,
    private val statsService: MinionStatsService,
    private val itemFactory: MinionItemFactory,
    private val limits: PlacementLimits,
    private val offlineProgress: OfflineProgressService,
    private val settings: SettingsHolder<MinionSettings>
) : MinionsManager {

    private val byUuid: MutableMap<UUID, Minion> = ConcurrentHashMap()
    private val byOwner: MutableMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

    override fun all(): Collection<Minion> = byUuid.values

    override fun getByUuid(uuid: UUID): Minion? = byUuid[uuid]

    override fun getByEntity(entity: Entity): Minion? =
        byUuid[entity.uniqueId] ?: byUuid.values.firstOrNull { it.entity.uniqueId == entity.uniqueId }

    override fun ownedBy(player: OfflinePlayer): Collection<Minion> =
        byOwner[player.uniqueId].orEmpty().mapNotNull { byUuid[it] }

    override fun countOwnedBy(player: OfflinePlayer): Int = byOwner[player.uniqueId]?.size ?: 0

    override fun placementLimit(player: OfflinePlayer): Int = limits.limitFor(player)

    override fun types(): Collection<MinionType> = types.all()

    override fun getType(id: String): MinionType? = types.find(id)

    override fun createItem(type: ApiMinionType, level: Int): ItemStack {
        val resolved = types.find(type.id)
            ?: throw IllegalArgumentException("Unknown minion type: ${type.id}")
        return itemFactory.build(MinionState(type = resolved, level = level))
    }

    fun register(minion: Minion): Minion {
        byUuid[minion.uuid] = minion
        minion.state.owner?.let { owner ->
            byOwner.computeIfAbsent(owner) { ConcurrentHashMap.newKeySet() }.add(minion.uuid)
        }
        return minion
    }

    fun unregister(minion: Minion) {
        byUuid.remove(minion.uuid)
        minion.state.owner?.let { byOwner[it]?.remove(minion.uuid) }
        minion.hologram?.remove()
        minion.hologram = null
    }

    /** Applies a state change and keeps derived stats in step with it. */
    fun updateState(minion: Minion, state: MinionState) {
        minion.state = state
        minion.stats = statsService.statsFor(state)
        repository.save(minion)
    }

    /** Recomputes stats for every minion, e.g. after upgrade configs are reloaded. */
    fun refreshAll() {
        for (minion in byUuid.values) {
            minion.stats = statsService.statsFor(minion.state)
        }
    }

    fun hydrateLoadedWorlds() {
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                hydrateChunk(chunk)
            }
        }
    }

    fun hydrateChunk(chunk: Chunk) {
        for (entity in chunk.entities) {
            if (entity is ArmorStand) {
                hydrate(entity)
            }
        }
    }

    /** @return The minion backed by [stand], loading it if it is not live yet. */
    fun hydrate(stand: ArmorStand): Minion? {
        getByEntity(stand)?.let { return it }

        val container = stand.persistentDataContainer
        val state = repository.read(container) ?: return null
        val minion = factory.create(state.copy(uuid = state.uuid ?: stand.uniqueId), stand)

        repository.readStoredFuel(container)?.let { (id, remaining) ->
            minion.burningFuelId = id
            minion.burningFuelRemainingMillis = remaining
        }

        val idleSince = state.shutdownTimestamp
        if (idleSince != null && settings.value.offlineEnabled) {
            offlineProgress.apply(minion, idleSince, System.currentTimeMillis())
        }

        return register(minion)
    }

    /** Persists and drops every minion in [chunk]; they rehydrate when it reloads. */
    fun dehydrateChunk(chunk: Chunk) {
        for (entity in chunk.entities) {
            val minion = getByEntity(entity) ?: continue
            repository.markShutdown(minion, System.currentTimeMillis())
            repository.save(minion)
            unregister(minion)
        }
    }

    fun shutdownAll() {
        val now = System.currentTimeMillis()
        for (minion in byUuid.values.toList()) {
            repository.markShutdown(minion, now)
            repository.save(minion)
            minion.hologram?.remove()
        }
        byUuid.clear()
        byOwner.clear()
    }
}
