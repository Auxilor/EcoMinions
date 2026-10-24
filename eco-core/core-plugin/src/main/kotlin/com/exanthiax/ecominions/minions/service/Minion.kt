package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionModel
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.ItemStack
import java.util.UUID
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion
import com.exanthiax.ecominions.api.minion.MinionDefinition as ApiMinionDefinition

/**
 * A minion that exists in the world: its persisted state, its stand, its storage
 * and the transient state its ticking needs.
 *
 * Deliberately behaviour-free. Mining, rendering and persistence are services
 * that act on a minion, not methods hanging off it.
 */
class Minion(
    state: MinionState,
    override val entity: ArmorStand,
    /**
     * The minion's storage. A [MinionStorage] rather than a Bukkit inventory
     * because capacity is upgraded a slot at a time, and nothing ever opens it.
     */
    val storage: MinionStorage,
    val stats: MinionStats
) : ApiMinion, FuelService.FuelBurner {

    var state: MinionState = state
        internal set

    /**
     * Grows or shrinks storage to [slots], preserving contents.
     *
     * Items that no longer fit after a shrink are returned rather than deleted -
     * the caller decides whether to drop them in the world.
     */
    fun resizeStorage(slots: Int): List<ItemStack> = storage.resize(slots)

    /**
     * Points this minion at a reloaded [definition].
     *
     * A placed minion holds the definition it was loaded with, so without this
     * a config reload never reaches it: new stats, targets or self-seeding only
     * took effect once its chunk reloaded.
     *
     * @return Items that no longer fit if the new type holds fewer slots.
     */
    fun rebind(definition: MinionDefinition): List<ItemStack> {
        state = state.copy(definition = definition)
        stats.rebase(definition.baseStats, state.level)
        return resizeStorage(stats.storageSize)
    }

    override val uuid: UUID = state.uuid ?: entity.uniqueId

    override val type: ApiMinionDefinition
        get() = state.definition

    override val level: Int
        get() = state.level

    override val xp: Double
        get() = state.xp

    override val owner: UUID?
        get() = state.owner

    override val location: Location
        get() = entity.location

    override val upgradeLevels: Map<String, Int>
        get() = state.upgrades

    override val storageSize: Int
        get() = storage.size

    override val usedStorage: Int
        get() = storage.used

    override val speedTicks: Int
        get() = stats.speedTicks

    override val efficiencyTicks: Int
        get() = stats.efficiencyTicks

    override val radius: Int
        get() = stats.radius

    override val height: Int
        get() = stats.height

    override val regenDelayTicks: Int
        get() = stats.regenDelayTicks

    override val storedExperience: Int
        get() = state.storedExperience

    var animation: AnimationState = AnimationState.IDLE
        internal set

    /** The custom model drawing this minion, or null when it shows its armour stand. */
    var model: MinionModel? = null
        internal set

    /** The model animation looping now, so it is only restarted when the state changes. */
    var playingAnimation: String? = null
        internal set

    /** Which `hologram.<reason>` line explains a blocked or waiting minion. Reset before each action. */
    var blockedReason: String = NO_TARGET
        internal set

    var hologram: TextDisplay? = null
        internal set

    /** Whether a reconcile has found this minion within its limits since it was loaded. */
    var seenAllowed: Boolean = false
        internal set

    override var burningFuelId: String? = null
        internal set

    override var burningFuelEndsAt: Long = 0L
        internal set

    /**
     * Fuel left when this minion went idle.
     *
     * Fuel only burns while a minion is active, so an absolute expiry is wrong
     * across an idle window: the remaining duration is what survives it.
     */
    var burningFuelRemainingMillis: Long = 0L
        internal set

    /** Converts a live expiry into a frozen remainder. Idempotent. */
    fun freezeFuel(now: Long) {
        if (burningFuelEndsAt <= 0L) {
            return
        }
        burningFuelRemainingMillis = (burningFuelEndsAt - now).coerceAtLeast(0L)
        burningFuelEndsAt = 0L
    }

    /** Converts a frozen remainder back into a live expiry. */
    fun thawFuel(now: Long) {
        if (burningFuelRemainingMillis <= 0L) {
            burningFuelId = null
            burningFuelEndsAt = 0L
            return
        }
        burningFuelEndsAt = now + burningFuelRemainingMillis
    }

    /** Transient per-tick bookkeeping, owned by [MinionTicker] and the activities. */
    val mining = MiningState()

    override val isStorageFull: Boolean
        get() = storage.isFull

    override fun toString(): String = "Minion(uuid=$uuid, type=${state.definition.id}, owner=${state.owner})"

    enum class AnimationState { IDLE, MINING, FULL, BLOCKED }

    companion object {
        const val NO_TARGET = "no-target"
    }
}
