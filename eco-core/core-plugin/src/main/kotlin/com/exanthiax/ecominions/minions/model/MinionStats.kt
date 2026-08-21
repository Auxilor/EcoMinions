package com.exanthiax.ecominions.minions.model

import java.util.UUID

/**
 * A minion's live stats: its type's base values plus whatever its active effects
 * currently contribute.
 *
 * Mutable by design. Permanent libreforge effects add a contribution when they
 * enable and drop it when they disable, so the accumulator is the single place a
 * minion's effective numbers exist. Clamps are applied on read, never when a
 * contribution lands - see [MinionStat].
 */
class MinionStats(
    private val base: MinionType.BaseStats
) {
    private val contributions = HashMap<MinionStat, MutableMap<UUID, Int>>()

    /** Records [amount] against [id], replacing any earlier amount for that id. */
    fun contribute(stat: MinionStat, id: UUID, amount: Int) {
        contributions.getOrPut(stat) { HashMap() }[id] = amount
    }

    fun remove(stat: MinionStat, id: UUID) {
        contributions[stat]?.remove(id)
    }

    fun clearContributions() {
        contributions.clear()
    }

    operator fun get(stat: MinionStat): Int {
        val raw = stat.clamp(baseOf(stat) + (contributions[stat]?.values?.sum() ?: 0))
        return if (stat == MinionStat.INVENTORY_SLOTS) {
            raw.coerceAtMost(base.maxStorageSlots)
        } else {
            raw
        }
    }

    private fun baseOf(stat: MinionStat): Int = when (stat) {
        MinionStat.SPEED_TICKS -> base.speedTicks
        MinionStat.EFFICIENCY_TICKS -> base.efficiencyTicks
        MinionStat.RADIUS -> base.radius
        MinionStat.INVENTORY_SLOTS -> base.storageSlots
    }

    val speedTicks: Int
        get() = this[MinionStat.SPEED_TICKS]

    val efficiencyTicks: Int
        get() = this[MinionStat.EFFICIENCY_TICKS]

    val radius: Int
        get() = this[MinionStat.RADIUS]

    /** Not upgradable, so it is read straight off the type. */
    val regenDelayTicks: Int
        get() = base.regenDelayTicks

    /** Capacity in stack slots, already capped by the type's own maximum. */
    val storageSize: Int
        get() = this[MinionStat.INVENTORY_SLOTS]

    /** @return Ticks per full mine cycle, used for offline simulation. */
    val cycleTicks: Int
        get() = (speedTicks + efficiencyTicks).coerceAtLeast(1)
}
