package com.exanthiax.ecominions.minions.model

import java.util.EnumMap
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
    base: MinionDefinition.BaseStats,
    level: Int
) {
    private var base: MinionDefinition.BaseStats = base
    private val contributions = HashMap<MinionStat, MutableMap<UUID, Int>>()
    private var baseCache = EnumMap<MinionStat, Int>(MinionStat::class.java)
    private var maxStorageSlotsCache = 0

    /**
     * The level base stats are evaluated at.
     *
     * Setting it recomputes the cache. Base stats are read every tick per
     * minion, so evaluating expressions per read is not affordable; level
     * changes are rare, so caching on write is.
     */
    var level: Int = level
        set(value) {
            field = value
            recomputeBase()
        }

    init {
        recomputeBase()
    }

    /**
     * Swaps the type's base values for [base] and re-evaluates them at [level].
     *
     * Contributions are kept: they belong to effects, which a reload refreshes
     * separately, so dropping them here would briefly strip every upgrade.
     */
    fun rebase(base: MinionDefinition.BaseStats, level: Int) {
        this.base = base
        this.level = level
    }

    private fun recomputeBase() {
        val recomputed = EnumMap<MinionStat, Int>(MinionStat::class.java)
        for (stat in MinionStat.entries) {
            recomputed[stat] = base.valueOf(stat, level)
        }
        baseCache = recomputed
        maxStorageSlotsCache = base.maxStorageSlotsAt(level)
    }

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
            raw.coerceAtMost(maxStorageSlotsCache)
        } else {
            raw
        }
    }

    private fun baseOf(stat: MinionStat): Int = baseCache[stat] ?: 0

    val speedTicks: Int
        get() = this[MinionStat.SPEED_TICKS]

    val efficiencyTicks: Int
        get() = this[MinionStat.EFFICIENCY_TICKS]

    val radius: Int
        get() = this[MinionStat.RADIUS]

    /** Vertical span of the mining box in blocks, from one below the minion upward. */
    val height: Int
        get() = this[MinionStat.HEIGHT]

    /** Not upgradable, so it is read straight off the type. */
    val regenDelayTicks: Int
        get() = base.regenDelayAt(level)

    /** Capacity in stack slots, already capped by the type's own maximum. */
    val storageSize: Int
        get() = this[MinionStat.INVENTORY_SLOTS]

    /** @return Ticks per full mine cycle, used for offline simulation. */
    val cycleTicks: Int
        get() = (speedTicks + efficiencyTicks).coerceAtLeast(1)
}
