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
    /** The id of another catalog recipe this one builds on, purely an ordering hint. */
    val higherLevelOf: String?
)
