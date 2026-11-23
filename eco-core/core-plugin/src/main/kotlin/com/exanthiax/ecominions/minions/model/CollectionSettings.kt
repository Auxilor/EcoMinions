package com.exanthiax.ecominions.minions.model

import wiki.ecohub.game.item.lookup.TestableItem

/** A collector's filter. An empty [targetItems] collects everything. */
data class CollectionSettings(
    val targetItems: List<TestableItem>,
    val collectExperience: Boolean
)
