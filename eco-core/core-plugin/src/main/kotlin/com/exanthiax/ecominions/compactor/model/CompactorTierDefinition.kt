package com.exanthiax.ecominions.compactor.model

import com.exanthiax.ecominions.upgrades.model.TierCost

/** One rung of a minion type's compactor ladder, before it is resolved against that type's ordering. */
class CompactorTierDefinition(
    val id: String,
    val costs: List<TierCost>,
    val recipes: List<CompactorRecipe>
)
