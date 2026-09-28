package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionUpgradeEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.libreforge.EmptyProvidedHolder
import org.bukkit.entity.Player

/**
 * Buying an upgrade for a minion.
 *
 * Lives in the minions slice because it mutates minion state; the upgrades slice
 * only supplies the definitions and the cost.
 */
class MinionUpgradeService(
    private val minions: MinionService,
    private val triggers: MinionTriggerDispatch,
    private val events: EventDispatcher
) {
    enum class Result { PURCHASED, CANNOT_AFFORD, NOT_ALLOWED, CONDITIONS_NOT_MET, CANCELLED }

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

        val event = MinionUpgradeEvent(minion, player, upgrade, tier.level)
        if (!events.dispatch(event)) {
            return Result.CANCELLED
        }

        cost.pay(player)
        applyTier(minion, upgrade, event.tier)
        triggers.upgraded(minion, upgrade.id, minion.state.upgradeLevel(upgrade.id))
        return Result.PURCHASED
    }

    /**
     * Sets [minion]'s tier of [upgrade] without charging anything.
     *
     * @return Whether the tier changed. False if [tier] is out of range or the
     *         event was cancelled.
     */
    fun setTier(minion: Minion, upgrade: Upgrade, tier: Int): Boolean {
        if (tier !in 0..upgrade.maxTier) {
            return false
        }

        val event = MinionUpgradeEvent(minion, null, upgrade, tier)
        if (!events.dispatch(event)) {
            return false
        }

        applyTier(minion, upgrade, event.tier)
        return true
    }

    private fun applyTier(minion: Minion, upgrade: Upgrade, tier: Int) {
        minions.updateState(minion, minion.state.withUpgrade(upgrade.id, tier.coerceIn(0, upgrade.maxTier)))
        minions.onLevelChanged(minion)
    }
}
