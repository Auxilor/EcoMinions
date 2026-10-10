package com.exanthiax.ecominions.upgrades.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.eco.core.price.impl.PriceFree
import com.willfp.eco.util.NamespacedKeyUtils
import com.willfp.libreforge.Holder
import com.willfp.libreforge.SimpleHolder
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects

/** Parses one file in `upgrades/` into an [Upgrade] and its tier ladder. */
class UpgradeLoader : ConfigLoader<Upgrade> {

    override fun load(id: String, config: Config, context: LoadContext): Upgrade? {
        val tiers = loadTiers(id, config, context)
        if (tiers.isEmpty()) {
            context.violation("tiers", "no valid tiers - the upgrade could never be bought")
            return null
        }

        return Upgrade(
            id = id,
            name = config.getFormattedString("name"),
            iconLookup = config.getStringOrNull("display.icon") ?: DEFAULT_ICON,
            tiers = tiers
        )
    }

    private fun loadTiers(id: String, config: Config, context: LoadContext): Map<Int, UpgradeTier> {
        val section = config.getSubsection("tiers")
        val tiers = mutableMapOf<Int, UpgradeTier>()

        for (key in section.getKeys(false)) {
            val level = key.toIntOrNull()
            if (level == null) {
                context.violation("tiers.$key", "tier keys must be whole numbers")
                continue
            }
            tiers[level] = readTier(
                id,
                level,
                config,
                section.getSubsection(key),
                context.with("tiers.$key")
            )
        }
        return tiers
    }

    private fun readTier(
        upgradeId: String,
        level: Int,
        upgradeConfig: Config,
        tierConfig: Config,
        context: LoadContext
    ): UpgradeTier {
        val value = tierConfig.getDoubleOrNull("value") ?: 0.0

        return UpgradeTier(
            level = level,
            value = value,
            statDelta = tierConfig.getStringOrNull("stat-delta") ?: "",
            costs = readCosts(tierConfig, context),
            holder = compileHolder(upgradeId, level, upgradeConfig, tierConfig, context),
            buyConditions = Conditions.compile(
                tierConfig.getSubsections("buy-conditions"),
                context.violationContext.with("buy-conditions")
            )
        )
    }

    /**
     * Compiles the upgrade's `effects:` block once per tier.
     *
     * A tier's own `effects:` block, if present, replaces the upgrade-level one
     * entirely rather than merging - two half-specified blocks would be far harder
     * for an admin to reason about than one that wins outright.
     */
    private fun compileHolder(
        upgradeId: String,
        level: Int,
        upgradeConfig: Config,
        tierConfig: Config,
        context: LoadContext
    ): Holder? {
        val effectConfigs = when {
            tierConfig.has("effects") -> tierConfig.getSubsections("effects")
            upgradeConfig.has("effects") -> upgradeConfig.getSubsections("effects")
            else -> return null
        }

        if (effectConfigs.isEmpty()) {
            return null
        }

        val violations = context.violationContext
        val effects = Effects.compile(effectConfigs, violations.with("effects"))
        val conditions = Conditions.compile(
            upgradeConfig.getSubsections("conditions"),
            violations.with("conditions")
        )

        return SimpleHolder(
            NamespacedKeyUtils.create(NAMESPACE, "upgrade_${upgradeId}_$level"),
            effects,
            conditions
        )
    }

    private fun readCosts(config: Config, context: LoadContext): List<TierCost> =
        config.getSubsections("cost-options").mapNotNull { costConfig ->
            val price = ConfiguredPrice.create(costConfig)
            if (price == null || price.price is PriceFree) {
                context.violation(
                    "cost-options",
                    "could not read a price from '${costConfig.getStringOrNull("type")}' - " +
                        "expected a registered price type such as coins, a material, or a currency id"
                )
                return@mapNotNull null
            }
            ConfiguredPriceCost(price)
        }

    private companion object {
        const val DEFAULT_ICON = "redstone"
        const val NAMESPACE = "ecominions"
    }
}
