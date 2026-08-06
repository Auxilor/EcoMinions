package com.exanthiax.ecominions.api.upgrade

/**
 * The stat effects an upgrade may declare.
 *
 * Published so that both the upgrade loader and the stat resolver validate against
 * one list, and so consumers can tell what an upgrade will do.
 */
object UpgradeEffects {
    const val SPEED = "speed"
    const val EFFICIENCY = "efficiency"
    const val FORTUNE = "fortune"
    const val RADIUS = "radius"
    const val CAPACITY = "capacity"
    const val AUTO_SMELT = "auto_smelt"
    const val AUTO_SELL = "auto_sell"
    const val EXPERIENCE = "experience"
    const val COMPACTOR = "compactor"

    val ALL: Set<String> = setOf(
        SPEED, EFFICIENCY, FORTUNE, RADIUS, CAPACITY,
        AUTO_SMELT, AUTO_SELL, EXPERIENCE, COMPACTOR
    )
}
