package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.fuel.service.FuelService
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
    val entity: ArmorStand,
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

    override val uuid: UUID = state.uuid ?: entity.uniqueId

    override val type: ApiMinionDefinition
        get() = state.type

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

    var animation: AnimationState = AnimationState.IDLE
        internal set

    var hologram: TextDisplay? = null
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

    /** Transient per-tick bookkeeping, owned by [MiningService]. */
    val mining = MiningState()

    val isStorageFull: Boolean
        get() = storage.isFull

    enum class AnimationState { IDLE, MINING, FULL, BLOCKED }
}
