package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionUpgradeEvent
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
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
    enum class Result { PURCHASED, CANNOT_AFFORD, NOT_ALLOWED }
    enum class ActivateResult { ACTIVATED, TIER_NOT_OWNED, NOT_ALLOWED }

    fun purchase(
        player: Player,
        minion: Minion,
        upgrade: Upgrade,
        tier: UpgradeTier,
        cost: TierCost
    ): Result {
        if (upgrade.id !in minion.state.type.allowedUpgradeIds) {
            return Result.NOT_ALLOWED
        }
        if (!cost.canAfford(player)) {
            return Result.CANNOT_AFFORD
        }

        cost.pay(player)
        var updated = minion.state.withUpgrade(upgrade.id, tier.level)
        if (upgrade.selectable) {
            updated = updated.withActiveTier(upgrade.id, tier.level)
        }
        minions.updateState(minion, updated)
        minions.onLevelChanged(minion)

        Bukkit.getPluginManager().callEvent(MinionUpgradeEvent(minion, player, upgrade, tier.level))
        return Result.PURCHASED
    }

    /** @return Whether [minion] could switch to running [tier] of a [selectable][Upgrade.selectable] upgrade. */
    fun activate(minion: Minion, upgrade: Upgrade, tier: Int): ActivateResult {
        if (upgrade.id !in minion.state.type.allowedUpgradeIds || !upgrade.selectable) {
            return ActivateResult.NOT_ALLOWED
        }
        if (tier > minion.state.upgradeLevel(upgrade.id)) {
            return ActivateResult.TIER_NOT_OWNED
        }

        minions.updateState(minion, minion.state.withActiveTier(upgrade.id, tier))
        // Holders are keyed off the active tier (see MinionHolders), so switching
        // tiers has to re-run the same refresh a purchase does.
        minions.onLevelChanged(minion)
        return ActivateResult.ACTIVATED
    }
}
