package com.exanthiax.ecominions.minions.model

/**
 * A minion stat that libreforge effects may contribute to.
 *
 * The bounds live here rather than in the effects, because they are properties of
 * the stat and must hold no matter which effect moved it. Applying them per
 * contribution would make the result depend on holder walk order.
 *
 * [INVENTORY_SLOTS] carries no maximum: its ceiling is declared per minion,
 * and an enum constant is shared by every minion on the server.
 */
enum class MinionStat(
    val min: Int,
    val max: Int?
) {
    SPEED_TICKS(min = 1, max = null),
    EFFICIENCY_TICKS(min = 0, max = null),
    RADIUS(min = 0, max = null),
    HEIGHT(min = 1, max = null),
    INVENTORY_SLOTS(min = 1, max = null);

    fun clamp(value: Int): Int {
        val floored = value.coerceAtLeast(min)
        return if (max == null) floored else floored.coerceAtMost(max)
    }
}
