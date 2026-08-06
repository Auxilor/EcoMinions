package com.exanthiax.ecominions.minions.model

import org.bukkit.Material

/**
 * Which blocks a minion may target, and how configured blocks map onto the block
 * that is actually placed in the world (`WHEAT_SEEDS` targets `WHEAT`).
 */
class GrowthRules(
    private val promotions: Map<Material, Material>,
    private val invalidTargets: Set<Material>,
    val stackedColumns: Set<Material>
) {
    /** @return The block [material] should be treated as, e.g. seeds to crop. */
    fun promote(material: Material): Material = promotions[material] ?: material

    fun isValidTarget(material: Material): Boolean = material !in invalidTargets

    /** Whether [material] grows upwards in a column, like sugar cane or bamboo. */
    fun isStackedColumn(material: Material): Boolean = material in stackedColumns

    companion object {
        val EMPTY = GrowthRules(emptyMap(), emptySet(), emptySet())
    }
}
