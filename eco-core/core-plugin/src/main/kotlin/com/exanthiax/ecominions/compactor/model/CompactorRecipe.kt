package com.exanthiax.ecominions.compactor.model

/** Compresses bulk drops into their block form inside minion storage. */
class CompactorRecipe(
    val id: String,
    /** eco item lookup string for the consumed item. */
    val inputLookup: String,
    val inputAmount: Int,
    /** eco item lookup string for the produced item. */
    val outputLookup: String,
    val outputAmount: Int
)
