package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.minion.MinionsManager
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.willfp.libreforge.forceRefreshHolders
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.OfflinePlayer
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.exanthiax.ecominions.api.minion.MinionDefinition as ApiMinionDefinition

/**
 * Owns the set of live minions: what exists, who owns it, and how stands in the
 * world become minions again after a restart.
 */
class MinionService(
    private val types: ConfigDrivenRegistry<MinionDefinition>,
    private val factory: MinionFactory,
    private val repository: MinionStateRepository,
    private val itemFactory: MinionItemFactory,
    private val limits: PlacementLimits,
    private val activity: MinionActivityService,
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

    override fun types(): Collection<MinionDefinition> = types.all()

    override fun getType(id: String): MinionDefinition? = types.find(id)

    override fun createItem(type: ApiMinionDefinition, level: Int): ItemStack {
        val resolved = types.find(type.id)
            ?: throw IllegalArgumentException("Unknown minion: ${type.id}")
        return itemFactory.build(MinionState(type = resolved, storedLevel = level))
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

    /** @return Every live minion owned by [owner]. Snapshotted, so callers may mutate. */
    fun ownedMinions(owner: UUID): List<Minion> =
        byOwner[owner].orEmpty().mapNotNull { byUuid[it] }

    /**
     * Applies a state change and routes it through [refreshEffects], so any holder
     * whose active/inactive status changed as a result has its stat contributions
     * updated via libreforge's enable/disable lifecycle.
     */
    fun updateState(minion: Minion, state: MinionState) {
        minion.state = state
        repository.save(minion)
        refreshEffects(minion)
    }

    /**
     * Refreshes every minion's active holders, e.g. after upgrade configs are
     * reloaded, letting libreforge's enable/disable lifecycle bring stat
     * contributions back in step with the new configuration.
     */
    fun refreshAll() {
        for (minion in byUuid.values) {
            refreshEffects(minion)
        }
    }

    /**
     * Recomputes which holders and effects apply.
     *
     * Called explicitly because libreforge only polls players, entities and the
     * global dispatcher - a minion dispatcher is never swept automatically.
     */
    fun refreshEffects(minion: Minion) {
        minion.toDispatcher().forceRefreshHolders()
    }

    /**
     * Brings everything derived from level back in step after a level change.
     *
     * Base stats are level-scaled, so storage capacity can move, which is
     * structural rather than merely numeric - the same resize path
     * `EffectMinionCapacity` uses applies here.
     */
    fun onLevelChanged(minion: Minion) {
        minion.stats.level = minion.state.level

        val overflow = minion.resizeStorage(minion.stats.storageSize)
        for (item in overflow) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }

        refreshEffects(minion)
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

        val registered = register(minion)
        if (activity.isActive(registered)) {
            activity.resume(registered)
        }
        refreshEffects(registered)
        return registered
    }

    /** Persists and drops every minion in [chunk]; they rehydrate when it reloads. */
    fun dehydrateChunk(chunk: Chunk) {
        for (entity in chunk.entities) {
            val minion = getByEntity(entity) ?: continue
            activity.markIdle(minion)
            repository.save(minion)
            unregister(minion)
        }
    }

    fun shutdownAll() {
        val now = System.currentTimeMillis()
        for (minion in byUuid.values.toList()) {
            activity.markIdle(minion, now)
            repository.save(minion)
            minion.hologram?.remove()
        }
        byUuid.clear()
        byOwner.clear()
    }
}
