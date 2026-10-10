package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.items.TestableItem

/** A collector's filter. An empty [targetItems] collects everything. */
data class CollectionSettings(
    val targetItems: List<TestableItem>,
    val collectExperience: Boolean
)
