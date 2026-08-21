package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.SimpleProvidedHolder
import com.willfp.libreforge.registerSpecificHolderProvider

/**
 * Everything that gives a minion effects: its type, and each purchased upgrade at
 * the tier it is running.
 *
 * Tier holders are pre-compiled per rung, so buying tier 3 swaps holder `..._2`
 * out for `..._3` and libreforge's own diffing disables and enables the right
 * permanent effects. There is no bespoke bookkeeping here on purpose.
 *
 * The tier a minion runs is not always the tier it owns: a selectable upgrade
 * lets a player run a lower tier than the one purchased, so the holder is
 * resolved through `MinionState.activeTierOf`, which falls back to the owned
 * tier for every non-selectable upgrade.
 */
class MinionHolders(
    private val upgradeById: (String) -> Upgrade?
) {
    fun holdersFor(minion: Minion): List<ProvidedHolder> {
        val state = minion.state
        val holders = mutableListOf<ProvidedHolder>()

        state.type.holder?.let { holders += SimpleProvidedHolder(it) }

        for (upgradeId in state.upgrades.keys) {
            if (upgradeId !in state.type.allowedUpgradeIds) {
                continue
            }
            val upgrade = upgradeById(upgradeId) ?: continue
            upgrade.holderFor(state.activeTierOf(upgradeId))?.let { holders += SimpleProvidedHolder(it) }
        }

        return holders
    }

    fun register() {
        registerSpecificHolderProvider<Minion> { holdersFor(it) }
    }
}
