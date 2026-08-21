package com.exanthiax.ecominions.compactor.integration

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.compactor.model.CompactorTierDefinition
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.upgrades.integration.ConfiguredPriceCost
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.price.ConfiguredPrice

/** Parses one file in `compactor-tiers/` into a [CompactorTierDefinition]. */
class CompactorTierDefinitionLoader : ConfigLoader<CompactorTierDefinition> {

    override fun load(id: String, config: Config, context: LoadContext): CompactorTierDefinition? {
        val recipes = readRecipes(id, config, context)
        if (recipes.isEmpty()) {
            context.violation("recipes", "no valid recipes - this tier would do nothing")
            return null
        }

        return CompactorTierDefinition(id = id, costs = readCosts(config, context), recipes = recipes)
    }

    /** Exposed for testing: parsing recipes needs no eco price registry, unlike [readCosts]. */
    fun readRecipes(tierId: String, config: Config, context: LoadContext): List<CompactorRecipe> {
        val section = config.getSubsection("recipes")
        val recipesContext = context.with("recipes")

        return section.getKeys(false).mapNotNull { key ->
            readRecipe(tierId, key, section.getSubsection(key), recipesContext.with(key))
        }
    }

    private fun readRecipe(tierId: String, key: String, config: Config, context: LoadContext): CompactorRecipe? {
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
            id = "${tierId}_$key",
            inputLookup = input,
            inputAmount = inputAmount,
            outputLookup = output,
            outputAmount = outputAmount
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
}
