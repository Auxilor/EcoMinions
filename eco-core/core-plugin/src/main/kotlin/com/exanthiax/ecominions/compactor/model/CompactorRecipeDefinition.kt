package com.exanthiax.ecominions.compactor.model

import com.exanthiax.ecominions.upgrades.model.Upgrade

/**
 * One catalog entry from a file in `compactor-recipes/`: the recipe itself,
 * and the single-tier [Upgrade] a minion buys to unlock it.
 */
class CompactorRecipeDefinition(
    val recipe: CompactorRecipe,
    val upgrade: Upgrade
)
