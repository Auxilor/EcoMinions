package com.exanthiax.ecominions.api.fuel

/** A consumable that temporarily speeds a minion up. */
interface Fuel {
    /** Config file name, without extension. */
    val id: String

    /** Formatted display name. */
    val displayName: String

    /** How long one unit burns for. */
    val durationTicks: Long

    /** Speed factor applied while burning; 1.25 means 25% faster. */
    val speedMultiplier: Double
}
