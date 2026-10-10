package com.exanthiax.ecominions.minions.model

/** Water a fishing minion needs, and an optional catch table replacing vanilla's. */
data class FishingSettings(
    val minWater: Int,
    val drops: MinionDropList
)
