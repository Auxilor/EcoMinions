package com.exanthiax.ecominions.minions.model

/** A minion's effective stats after its purchased upgrades are applied. */
data class MinionStats(
    val speedTicks: Int,
    val efficiencyTicks: Int,
    val radius: Int,
    val inventoryRows: Int,
    val regenDelayTicks: Int,
    val fortuneMultiplier: Double,
    val autoSmelt: Boolean,
    val autoSell: Boolean,
    val xpDropAmount: Int,
    val compactorEnabled: Boolean
) {
    val storageSize: Int
        get() = inventoryRows * 9

    /** @return Ticks per full mine cycle, used for offline simulation. */
    val cycleTicks: Int
        get() = (speedTicks + efficiencyTicks).coerceAtLeast(1)
}

/** One purchased upgrade tier, reduced to what stat resolution needs. */
data class UpgradeEffect(
    val effect: String,
    val value: Double
)
