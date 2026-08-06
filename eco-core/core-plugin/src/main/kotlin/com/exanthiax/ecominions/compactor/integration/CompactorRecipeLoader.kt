package com.exanthiax.ecominions.compactor.integration

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.willfp.eco.core.config.interfaces.Config

/** Parses one file in `compactor-recipes/` into a [CompactorRecipe]. */
class CompactorRecipeLoader : ConfigLoader<CompactorRecipe> {

    override fun load(id: String, config: Config, context: LoadContext): CompactorRecipe? {
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
            outputAmount = outputAmount
        )
    }
}
