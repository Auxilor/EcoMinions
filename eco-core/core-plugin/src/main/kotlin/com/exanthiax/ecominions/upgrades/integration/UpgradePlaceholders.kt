package com.exanthiax.ecominions.upgrades.integration

import com.exanthiax.ecominions.core.placeholders.InternalPlaceholder
import com.exanthiax.ecominions.core.placeholders.PlaceholderSource
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier

/** The upgrades slice's internal placeholders. */
class UpgradePlaceholders : PlaceholderSource {

    override fun internalPlaceholders(): List<InternalPlaceholder> = listOf(
        InternalPlaceholder("upgrade_name") { it.get<Upgrade>(UPGRADE)?.name },
        InternalPlaceholder("max_tier") { it.get<Upgrade>(UPGRADE)?.maxTier?.toString() },
        InternalPlaceholder("delta") { it.get<UpgradeTier>(TIER)?.statDelta }
    )

    companion object {
        /** Context keys the upgrade placeholders resolve against. */
        const val UPGRADE = "upgrade"
        const val TIER = "tier_object"
    }
}
