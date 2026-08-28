package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.SimpleProvidedHolder
import com.willfp.libreforge.registerSpecificHolderProvider

/**
 * Everything that gives a minion effects: its type, and each purchased upgrade at
 * the tier actually owned.
 *
 * Tier holders are pre-compiled per rung, so buying tier 3 swaps holder `..._2`
 * out for `..._3` and libreforge's own diffing disables and enables the right
 * permanent effects. There is no bespoke bookkeeping here on purpose.
 */
class MinionHolders(
    private val upgradeById: (String) -> Upgrade?
) {
    fun holdersFor(minion: Minion): List<ProvidedHolder> {
        val state = minion.state
        val holders = mutableListOf<ProvidedHolder>()

        state.definition.holder?.let { holders += SimpleProvidedHolder(it) }

        for ((upgradeId, tier) in state.upgrades) {
            if (upgradeId !in state.definition.allowedUpgradeIds) {
                continue
            }
            val upgrade = upgradeById(upgradeId) ?: continue
            upgrade.holderFor(tier)?.let { holders += SimpleProvidedHolder(it) }
        }

        return holders
    }

    fun register() {
        registerSpecificHolderProvider<Minion> { holdersFor(it) }
    }
}
