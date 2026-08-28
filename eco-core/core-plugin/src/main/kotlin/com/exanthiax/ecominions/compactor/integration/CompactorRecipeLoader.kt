package com.exanthiax.ecominions.compactor.integration

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.upgrades.integration.ConfiguredPriceCost
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.conditions.Conditions

/** Parses one file in `compactor-recipes/` into a compaction rule and the [Upgrade] that unlocks it. */
class CompactorRecipeLoader : ConfigLoader<CompactorRecipeDefinition> {

    override fun load(id: String, config: Config, context: LoadContext): CompactorRecipeDefinition? {
        val violations = context.violationContext
        val conditions = Conditions.compile(config.getSubsections("conditions"), violations.with("conditions"))
        val recipe = readRecipe(id, config, context, conditions) ?: return null

        val costs = readCosts(config, context)
        if (costs.isEmpty()) {
            context.violation("cost-options", "no valid prices - this recipe could never be bought")
            return null
        }

        val buyConditions = Conditions.compile(
            config.getSubsections("buy-conditions"),
            violations.with("buy-conditions")
        )

        val upgrade = Upgrade(
            id = "compactor_$id",
            name = config.getFormattedString("name"),
            iconLookup = config.getStringOrNull("display.icon") ?: DEFAULT_ICON,
            tiers = mapOf(
                1 to UpgradeTier(
                    level = 1,
                    value = 1.0,
                    statDelta = "Enabled",
                    costs = costs,
                    holder = null,
                    buyConditions = buyConditions
                )
            )
        )

        return CompactorRecipeDefinition(recipe, upgrade)
    }

    /** Exposed for testing: [conditions] pre-compiled so a test can pass an empty [ConditionList]. */
    fun readRecipe(id: String, config: Config, context: LoadContext, conditions: ConditionList): CompactorRecipe? {
        val input = config.getStringOrNull("input")
        if (input.isNullOrBlank()) {
            context.violation("input", "no input item configured")
            return null
        }

        val output = config.getStringOrNull("output")
        if (output.isNullOrBlank()) {
            context.violation("output", "no output item configured")
            return null
        }

        val inputAmount = config.getInt("input-amount")
        if (inputAmount < 1) {
            context.violation("input-amount", "must be at least 1, was $inputAmount")
            return null
        }

        val outputAmount = config.getInt("output-amount")
        if (outputAmount < 1) {
            context.violation("output-amount", "must be at least 1, was $outputAmount")
            return null
        }

        return CompactorRecipe(
            id = id,
            inputLookup = input,
            inputAmount = inputAmount,
            outputLookup = output,
            outputAmount = outputAmount,
            conditions = conditions,
            higherLevelOf = config.getStringOrNull("higher-level-of")?.takeIf { it.isNotBlank() }
        )
    }

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
        const val DEFAULT_ICON = "furnace"
    }
}
