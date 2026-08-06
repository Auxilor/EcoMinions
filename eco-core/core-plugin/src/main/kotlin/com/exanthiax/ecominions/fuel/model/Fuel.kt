package com.exanthiax.ecominions.fuel.model

import com.exanthiax.ecominions.api.fuel.Fuel as ApiFuel

/** A consumable that temporarily speeds a minion up. */
class Fuel(
    override val id: String,
    override val displayName: String,
    /** eco item lookup string for the item that counts as this fuel. */
    val itemLookup: String,
    override val durationTicks: Long,
    override val speedMultiplier: Double
) : ApiFuel {

    /** @return [baseTicks] shortened by this fuel, never below one tick. */
    fun applyTo(baseTicks: Int): Int = (baseTicks / speedMultiplier).toInt().coerceAtLeast(1)
}
