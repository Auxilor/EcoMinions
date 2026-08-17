package com.exanthiax.ecominions.upgrades.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.price.ConfiguredPrice

/** Parses one file in `upgrades/` into an [Upgrade] and its tier ladder. */
class UpgradeLoader : ConfigLoader<Upgrade> {

    override fun load(id: String, config: Config, context: LoadContext): Upgrade? {
        val effect = config.getString("effect").lowercase()

        val tiers = loadTiers(config, context)
        if (tiers.isEmpty()) {
            context.violation("tiers", "no valid tiers - the upgrade could never be bought")
            return null
        }

        return Upgrade(
            id = id,
            name = config.getFormattedString("name"),
            effect = effect,
            iconLookup = config.getStringOrNull("display.icon") ?: DEFAULT_ICON,
            tiers = tiers
        )
    }

    private fun loadTiers(config: Config, context: LoadContext): Map<Int, UpgradeTier> {
        val section = config.getSubsection("tiers")
        val tiers = mutableMapOf<Int, UpgradeTier>()

        for (key in section.getKeys(false)) {
            val level = key.toIntOrNull()
            if (level == null) {
                context.violation("tiers.$key", "tier keys must be whole numbers")
                continue
            }
            tiers[level] = readTier(level, section.getSubsection(key), context.with("tiers.$key"))
        }
        return tiers
    }

    private fun readTier(level: Int, config: Config, context: LoadContext): UpgradeTier =
        UpgradeTier(
            level = level,
            value = config.getDoubleOrNull("value") ?: 0.0,
            statDelta = config.getStringOrNull("stat-delta") ?: "",
            costs = readCosts(config, context)
        )

    private fun readCosts(config: Config, context: LoadContext): List<TierCost> =
        config.getSubsections("cost-options").mapNotNull { costConfig ->
            val price = ConfiguredPrice.create(costConfig)
            if (price == null) {
                context.violation(
                    "cost-options",
                    "could not read a price from '${costConfig.getStringOrNull("type")}' - " +
                        "expected a registered price type such as vault, a material, or a currency id"
                )
                return@mapNotNull null
            }
            ConfiguredPriceCost(price)
        }

    private companion object {
        const val DEFAULT_ICON = "redstone"
    }
}
