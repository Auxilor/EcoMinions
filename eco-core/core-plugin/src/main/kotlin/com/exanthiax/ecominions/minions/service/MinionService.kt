package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionLoadEvent
import com.exanthiax.ecominions.api.event.MinionUnloadEvent
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.willfp.libreforge.flushHolders
import com.willfp.libreforge.forceRefreshHolders
import com.willfp.libreforge.updateEffects
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns the set of live minions: what exists, who owns it, and how stands in the
 * world become minions again after a restart.
 */
class MinionService(
    private val types: ConfigDrivenRegistry<MinionDefinition>,
    private val factory: MinionFactory,
    private val repository: MinionStateRepository,
    private val limits: PlacementLimits,
    private val activity: MinionActivityService,
    private val settings: SettingsHolder<MinionSettings>,
    private val events: EventDispatcher,
    private val regions: Regions,
    private val placed: PlacedMinionRegistry,
    private val onRegister: (Minion) -> Unit = {},
    private val onRetire: (Minion) -> Unit = {}
) {

    private val byUuid: MutableMap<UUID, Minion> = ConcurrentHashMap()
    private val byOwner: MutableMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

    fun all(): Collection<Minion> = byUuid.values

    fun getByUuid(uuid: UUID): Minion? = byUuid[uuid]

    fun getByEntity(entity: Entity): Minion? =
        byUuid[entity.uniqueId] ?: byUuid.values.firstOrNull { it.entity.uniqueId == entity.uniqueId }

    fun ownedBy(player: OfflinePlayer): Collection<Minion> =
        byOwner[player.uniqueId].orEmpty().mapNotNull { byUuid[it] }

    /** Counts loaded and unloaded minions alike, from the owner's saved data. */
    fun countOwnedBy(player: OfflinePlayer): Int = placed.count(player.uniqueId)

    fun countOwnedBy(player: OfflinePlayer, typeId: String): Int = placed.count(player.uniqueId, typeId)

    fun placementLimit(player: OfflinePlayer): Int = limits.limitFor(player)

    fun types(): Collection<MinionDefinition> = types.all()

    fun getType(id: String): MinionDefinition? = types.find(id)

    /** Also records the minion in its owner's data, which backfills minions placed before the registry existed. */
    fun register(minion: Minion): Minion {
        byUuid[minion.uuid] = minion
        minion.state.owner?.let { owner ->
            byOwner.computeIfAbsent(owner) { ConcurrentHashMap.newKeySet() }.add(minion.uuid)
            placed.add(owner, minion.uuid, minion.state.definition.id)
        }
        onRegister(minion)
        return minion
    }

    /** Unregisters a minion that is leaving the world for good, so its owner no longer counts it. */
    fun retire(minion: Minion) {
        minion.state.owner?.let { placed.remove(it, minion.uuid) }
        onRetire(minion)
        unregister(minion)
    }

    /**
     * Forgets every minion [owner] placed except [keep]; any still in the world
     * record themselves again when they load.
     */
    fun forgetPlaced(owner: UUID, keep: Set<UUID> = emptySet()) = placed.clear(owner, keep)

    fun unregister(minion: Minion) {
        byUuid.remove(minion.uuid)
        minion.state.owner?.let { byOwner[it]?.remove(minion.uuid) }
        minion.hologram?.remove()
        minion.hologram = null
    }

    /** Moves [minion] to [owner], or makes it unowned, keeping the owner index in step. */
    fun changeOwner(minion: Minion, owner: UUID?) {
        minion.state.owner?.let {
            byOwner[it]?.remove(minion.uuid)
            placed.remove(it, minion.uuid)
        }
        owner?.let {
            byOwner.computeIfAbsent(it) { ConcurrentHashMap.newKeySet() }.add(minion.uuid)
            placed.add(it, minion.uuid, minion.state.definition.id)
        }
        updateState(minion, minion.state.copy(owner = owner))

        if (activity.isActive(minion)) {
            activity.resume(minion)
        } else {
            activity.markIdle(minion)
        }
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
     * Brings every live minion in step with reloaded configs: its type
     * definition first, then its active holders, letting libreforge's
     * enable/disable lifecycle bring stat contributions back in step.
     *
     * A minion whose type no longer exists keeps the definition it has, so it
     * carries on until the next restart rather than breaking mid-session.
     *
     * Each minion is refreshed on the region that owns its stand.
     */
    fun refreshAll() {
        for (minion in byUuid.values) {
            regions.on(minion.entity) { refresh(minion) }
        }
    }

    private fun refresh(minion: Minion) {
        val reloaded = types.find(minion.state.definition.id)
        if (reloaded != null && reloaded !== minion.state.definition) {
            for (item in minion.rebind(reloaded)) {
                minion.entity.world.dropItemNaturally(minion.entity.location, item)
            }
        }
        refreshEffects(minion)
    }

    /**
     * Recomputes which holders and effects apply.
     *
     * Called explicitly because libreforge only polls players, entities and the
     * global dispatcher - a minion dispatcher is never swept automatically.
     * Conditions are re-evaluated too, as unchanged holders keep their old
     * results, and the change is applied now so a trigger fired straight
     * after (such as minion_place) already sees the holders.
     */
    fun refreshEffects(minion: Minion) {
        minion.toDispatcher().apply {
            forceRefreshHolders()
            updateEffects()
            flushHolders()
        }
    }

    /**
     * Brings everything derived from level back in step after a level change.
     *
     * Base stats are level-scaled, so storage capacity can move, which is
     * structural rather than merely numeric - the same resize path
     * `EffectMinionCapacity` uses applies here.
     *
     * Runs on the region that owns the minion's stand, as the overflow is dropped there.
     */
    fun onLevelChanged(minion: Minion) = regions.on(minion.entity) {
        minion.stats.level = minion.state.level

        val overflow = minion.resizeStorage(minion.stats.storageSize)
        for (item in overflow) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }

        refreshEffects(minion)
    }

    /**
     * Each chunk is hydrated on the region that owns it. Runs on every reload,
     * as minion types only exist once configs have loaded; live minions are skipped.
     */
    fun hydrateLoadedWorlds() {
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                regions.at(world, chunk.x, chunk.z) {
                    if (world.isChunkLoaded(chunk.x, chunk.z)) {
                        hydrateEntities(chunk.entities.asList())
                    }
                }
            }
        }
    }

    fun hydrateEntities(entities: List<Entity>) {
        for (entity in entities) {
            if (entity is ArmorStand) {
                hydrate(entity)
            }
        }
    }

    /** @return The minion backed by [stand], loading it if it is not live yet. */
    fun hydrate(stand: ArmorStand): Minion? {
        getByEntity(stand)?.let { return it }

        val state = repository.read(stand.persistentDataContainer) ?: return null
        val minion = factory.create(state.copy(uuid = state.uuid ?: stand.uniqueId), stand)
        minion.mining.seeded += repository.readSeeded(stand)

        val registered = register(minion)
        if (activity.isActive(registered)) {
            activity.resume(registered)
        }
        refreshEffects(registered)
        events.dispatch(MinionLoadEvent(registered))
        return registered
    }

    /** Persists and drops every minion in [entities]; they rehydrate when they load again. */
    fun dehydrateEntities(entities: List<Entity>) {
        for (entity in entities) {
            val minion = getByEntity(entity) ?: continue
            events.dispatch(MinionUnloadEvent(minion))
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
