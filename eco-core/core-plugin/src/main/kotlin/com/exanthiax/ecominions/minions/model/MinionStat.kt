package com.exanthiax.ecominions.minions.model

/** Enum constants initialize before any companion object, so this bound must live outside it. */
private const val MAX_INVENTORY_ROWS = 6

/**
 * A minion stat that libreforge effects may contribute to.
 *
 * The bounds live here rather than in the effects, because they are properties of
 * the stat and must hold no matter which effect moved it. Applying them per
 * contribution would make the result depend on holder walk order.
 */
enum class MinionStat(
    val min: Int,
    val max: Int?
) {
    SPEED_TICKS(min = 1, max = null),
    EFFICIENCY_TICKS(min = 0, max = null),
    RADIUS(min = 0, max = null),
    INVENTORY_ROWS(min = 1, max = MAX_INVENTORY_ROWS);

    fun clamp(value: Int): Int {
        val floored = value.coerceAtLeast(min)
        return if (max == null) floored else floored.coerceAtMost(max)
    }
}
