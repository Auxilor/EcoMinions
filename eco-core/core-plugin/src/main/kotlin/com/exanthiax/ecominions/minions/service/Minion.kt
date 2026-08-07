package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.Inventory
import java.util.UUID
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion
import com.exanthiax.ecominions.api.minion.MinionType as ApiMinionType

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
    val storage: Inventory,
    stats: MinionStats
) : ApiMinion, FuelService.FuelBurner {

    var state: MinionState = state
        internal set

    var stats: MinionStats = stats
        internal set

    override val uuid: UUID = state.uuid ?: entity.uniqueId

    override val type: ApiMinionType
        get() = state.type

    override val level: Int
        get() = state.level

    override val owner: UUID?
        get() = state.owner

    override val location: Location
        get() = entity.location

    override val upgradeLevels: Map<String, Int>
        get() = state.upgrades

    override val storageSize: Int
        get() = storage.size

    override val usedStorage: Int
        get() = (0 until storage.size).count { storage.getItem(it) != null }

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
        get() = (0 until storage.size).none { storage.getItem(it) == null }

    enum class AnimationState { IDLE, MINING, FULL, BLOCKED }
}
