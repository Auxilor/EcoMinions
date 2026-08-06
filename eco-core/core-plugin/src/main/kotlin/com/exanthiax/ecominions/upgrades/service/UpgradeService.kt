package com.exanthiax.ecominions.upgrades.service

import com.exanthiax.ecominions.api.upgrade.UpgradesManager
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion
import com.exanthiax.ecominions.api.upgrade.Upgrade as ApiUpgrade

/**
 * Queries over configured upgrades.
 *
 * Read-only by design: applying a purchase changes minion state, so it belongs to
 * the minions slice.
 */
class UpgradeService(
    private val upgrades: ConfigDrivenRegistry<Upgrade>
) : UpgradesManager {

    override fun all(): Collection<Upgrade> = upgrades.all()

    override fun getById(id: String): Upgrade? = upgrades.find(id)

    override fun allowedFor(minion: ApiMinion): List<Upgrade> =
        minion.type.allowedUpgradeIds.mapNotNull { getById(it) }

    override fun tierOf(minion: ApiMinion, upgrade: ApiUpgrade): Int =
        minion.upgradeLevels[upgrade.id] ?: 0

    override fun effectValue(upgrade: ApiUpgrade, tier: Int): Double? =
        getById(upgrade.id)?.tier(tier)?.value

    /** @return The tier [minion] could buy next, or null if it is maxed out. */
    fun nextTier(minion: ApiMinion, upgrade: Upgrade): UpgradeTier? =
        upgrade.nextTier(tierOf(minion, upgrade))
}
