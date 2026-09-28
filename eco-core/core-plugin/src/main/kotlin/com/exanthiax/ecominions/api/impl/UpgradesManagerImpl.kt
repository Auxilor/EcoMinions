package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.upgrade.PurchaseResult
import com.exanthiax.ecominions.api.upgrade.UpgradesManager
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.service.UpgradeService
import org.bukkit.entity.Player
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion
import com.exanthiax.ecominions.api.upgrade.Upgrade as ApiUpgrade

/** The public upgrades API, over the upgrade registry and the purchase flow. */
class UpgradesManagerImpl(
    private val upgrades: UpgradeService,
    private val purchases: MinionUpgradeService,
    private val minions: MinionService
) : UpgradesManager {

    override fun all(): Collection<ApiUpgrade> = upgrades.all()

    override fun getById(id: String): ApiUpgrade? = upgrades.getById(id)

    override fun allowedFor(minion: ApiMinion): List<ApiUpgrade> = upgrades.allowedFor(minion)

    override fun tierOf(minion: ApiMinion, upgrade: ApiUpgrade): Int = upgrades.tierOf(minion, upgrade)

    override fun effectValue(upgrade: ApiUpgrade, tier: Int): Double? = upgrades.effectValue(upgrade, tier)

    override fun setTier(minion: ApiMinion, upgrade: ApiUpgrade, tier: Int): Boolean {
        val resolved = resolve(minion)
        return purchases.setTier(resolved, resolve(resolved, upgrade), tier)
    }

    override fun purchase(player: Player, minion: ApiMinion, upgrade: ApiUpgrade, tier: Int): PurchaseResult {
        val resolved = resolve(minion)
        val internal = resolve(resolved, upgrade)
        if (tier != upgrades.tierOf(resolved, internal) + 1) {
            return PurchaseResult.INVALID_TIER
        }
        val next = internal.tier(tier) ?: return PurchaseResult.INVALID_TIER
        if (next.costs.isEmpty()) {
            return PurchaseResult.NOT_ALLOWED
        }

        val cost = next.costs.firstOrNull { it.canAfford(player) } ?: next.costs.first()
        return when (purchases.purchase(player, resolved, internal, next, cost)) {
            MinionUpgradeService.Result.PURCHASED -> PurchaseResult.PURCHASED
            MinionUpgradeService.Result.CANNOT_AFFORD -> PurchaseResult.CANNOT_AFFORD
            MinionUpgradeService.Result.NOT_ALLOWED -> PurchaseResult.NOT_ALLOWED
            MinionUpgradeService.Result.CONDITIONS_NOT_MET -> PurchaseResult.CONDITIONS_NOT_MET
            MinionUpgradeService.Result.CANCELLED -> PurchaseResult.CANCELLED
        }
    }

    override fun resetUpgrades(minion: ApiMinion) {
        val resolved = resolve(minion)
        for ((id, tier) in resolved.state.upgrades) {
            if (tier <= 0) {
                continue
            }
            val upgrade = findUpgrade(resolved, id) ?: continue
            purchases.setTier(resolved, upgrade, 0)
        }
    }

    private fun resolve(minion: ApiMinion): Minion =
        minions.getByUuid(minion.uuid)
            ?: throw IllegalArgumentException("Minion ${minion.uuid} is not loaded")

    private fun resolve(minion: Minion, upgrade: ApiUpgrade): Upgrade =
        findUpgrade(minion, upgrade.id)
            ?: throw IllegalArgumentException("Unknown upgrade: ${upgrade.id}")

    /** Compactor upgrades belong to a minion type rather than the registry, so both are searched. */
    private fun findUpgrade(minion: Minion, id: String): Upgrade? =
        upgrades.getById(id)
            ?: minion.state.definition.compactorRecipes.map { it.upgrade }.firstOrNull { it.id == id }
}
