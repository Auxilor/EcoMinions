package com.exanthiax.ecominions.compactor.model

import com.willfp.libreforge.conditions.ConditionList

/** Compresses bulk drops into their block form inside minion storage. */
class CompactorRecipe(
    val id: String,
    /** eco item lookup string for the consumed item. */
    val inputLookup: String,
    val inputAmount: Int,
    /** eco item lookup string for the produced item. */
    val outputLookup: String,
    val outputAmount: Int,
    /** Gates whether this recipe currently compacts, checked each time a minion tries. */
    val conditions: ConditionList,
    /**
     * The id of another catalog recipe this one builds on, e.g. an
     * `enchanted_coal` recipe consuming `COAL_BLOCK` declares `higher-level-of:
     * coal_block`. Purely an ordering hint for [com.exanthiax.ecominions.compactor.service.CompactorService] -
     * it does not gate purchase or ownership on its own.
     */
    val higherLevelOf: String?
)
