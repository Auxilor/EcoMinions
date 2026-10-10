package com.exanthiax.ecominions.minions.model

/** Fuel a minion was burning, as the time left on it rather than a clock time. */
data class StoredFuel(
    val id: String,
    val remainingMillis: Long
)
