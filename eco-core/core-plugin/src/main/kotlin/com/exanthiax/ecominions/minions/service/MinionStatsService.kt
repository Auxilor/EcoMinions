package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.upgrade.UpgradesManager
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStatResolver
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.UpgradeEffect

/**
 * Resolves a minion's effective stats.
 *
 * Reaches the upgrades slice only through its published manager interface, so the
 * two slices never see each other's internals.
 */
class MinionStatsService(
    private val upgrades: UpgradesManager
) {
    fun statsFor(state: MinionState): MinionStats =
        MinionStatResolver.resolve(state.type.baseStats, effectsOf(state))

    private fun effectsOf(state: MinionState): List<UpgradeEffect> =
        state.upgrades.mapNotNull { (upgradeId, tier) ->
            val upgrade = upgrades.getById(upgradeId) ?: return@mapNotNull null
            if (upgradeId !in state.type.allowedUpgradeIds) {
                return@mapNotNull null
            }
            val value = upgrades.effectValue(upgrade, tier) ?: return@mapNotNull null
            UpgradeEffect(upgrade.effect, value)
        }
}
