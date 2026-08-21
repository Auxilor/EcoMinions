package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
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
    storage: Inventory,
    val stats: MinionStats
) : ApiMinion, FuelService.FuelBurner {

    var state: MinionState = state
        internal set

    /**
     * The minion's storage.
     *
     * Reassignable because the capacity effect can change the row count at any
     * refresh, not only at purchase time. [resizeStorage] is the only writer.
     */
    var storage: Inventory = storage
        private set

    /**
     * Grows or shrinks storage to [slots], preserving contents.
     *
     * Items that no longer fit after a shrink are returned rather than deleted -
     * the caller decides whether to drop them in the world.
     */
    fun resizeStorage(slots: Int, title: String): List<ItemStack> {
        // A chest inventory's size must be a multiple of nine; rounding up is the
        // closest a Bukkit inventory gets to an exact slot count.
        val size = ((slots + 8) / 9) * 9
        if (size == storage.size) {
            return emptyList()
        }

        val contents = (0 until storage.size).mapNotNull { storage.getItem(it) }
        val resized = Bukkit.createInventory(null, size, title)
        val overflow = contents.flatMap { resized.addItem(it).values }

        for (viewer in storage.viewers.toList()) {
            viewer.closeInventory()
        }

        storage = resized
        return overflow
    }

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
