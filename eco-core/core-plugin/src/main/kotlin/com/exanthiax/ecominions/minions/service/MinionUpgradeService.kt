package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionUpgradeEvent
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.libreforge.EmptyProvidedHolder
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Buying an upgrade for a minion.
 *
 * Lives in the minions slice because it mutates minion state; the upgrades slice
 * only supplies the definitions and the cost.
 */
class MinionUpgradeService(
    private val minions: MinionService
) {
    enum class Result { PURCHASED, CANNOT_AFFORD, NOT_ALLOWED, CONDITIONS_NOT_MET }

    fun purchase(
        player: Player,
        minion: Minion,
        upgrade: Upgrade,
        tier: UpgradeTier,
        cost: TierCost
    ): Result {
        if (upgrade.id !in minion.state.definition.allowedUpgradeIds) {
            return Result.NOT_ALLOWED
        }
        if (tier.buyConditions.isNotEmpty() && !tier.buyConditions.areMet(minion.toDispatcher(), EmptyProvidedHolder)) {
            return Result.CONDITIONS_NOT_MET
        }
        if (!cost.canAfford(player)) {
            return Result.CANNOT_AFFORD
        }

        cost.pay(player)
        minions.updateState(minion, minion.state.withUpgrade(upgrade.id, tier.level))
        minions.onLevelChanged(minion)

        Bukkit.getPluginManager().callEvent(MinionUpgradeEvent(minion, player, upgrade, tier.level))
        return Result.PURCHASED
    }
}
