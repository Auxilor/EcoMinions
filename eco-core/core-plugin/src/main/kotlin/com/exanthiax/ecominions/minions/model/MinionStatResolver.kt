package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.api.upgrade.UpgradeEffects

/**
 * Applies purchased upgrade effects to a type's base stats.
 *
 * Pure: given the same base stats and effects it always produces the same result,
 * which is what makes stat regressions catchable in a unit test.
 */
object MinionStatResolver {

    fun resolve(base: MinionType.BaseStats, effects: List<UpgradeEffect>): MinionStats {
        var speedTicks = base.speedTicks
        var efficiencyTicks = base.efficiencyTicks
        var radius = base.radius
        var inventoryRows = base.inventoryRows
        var fortune = 1.0
        var autoSmelt = false
        var autoSell = false
        var xpDropAmount = 0
        var compactorEnabled = false

        for ((effect, value) in effects) {
            when (effect) {
                UpgradeEffects.SPEED -> speedTicks = (speedTicks - value.toInt()).coerceAtLeast(1)
                UpgradeEffects.EFFICIENCY -> efficiencyTicks = (efficiencyTicks - value.toInt()).coerceAtLeast(0)
                UpgradeEffects.FORTUNE -> fortune *= (1.0 + value)
                UpgradeEffects.RADIUS -> radius += value.toInt()
                UpgradeEffects.CAPACITY -> inventoryRows = (inventoryRows + value.toInt()).coerceIn(1, MAX_ROWS)
                UpgradeEffects.AUTO_SMELT -> autoSmelt = value > 0
                UpgradeEffects.AUTO_SELL -> autoSell = value > 0
                UpgradeEffects.EXPERIENCE -> xpDropAmount = value.toInt()
                UpgradeEffects.COMPACTOR -> compactorEnabled = value > 0
            }
        }

        return MinionStats(
            speedTicks = speedTicks,
            efficiencyTicks = efficiencyTicks,
            radius = radius,
            inventoryRows = inventoryRows,
            regenDelayTicks = base.regenDelayTicks,
            fortuneMultiplier = fortune,
            autoSmelt = autoSmelt,
            autoSell = autoSell,
            xpDropAmount = xpDropAmount,
            compactorEnabled = compactorEnabled
        )
    }

    private const val MAX_ROWS = 6
}
