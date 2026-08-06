package com.exanthiax.ecominions.minions.model

import org.bukkit.Material

/** A tree species: what a tree minion plants, fells and harvests. */
data class WoodFamily(
    val id: String,
    val sapling: Material,
    val log: Material,
    val leaves: Material,
    val defaultSurface: Material?,
    val members: Set<Material>
)

/** Every known tree species, looked up by the blocks a type targets. */
class WoodFamilies(
    private val families: List<WoodFamily>
) {
    fun byMaterial(material: Material): WoodFamily? =
        families.firstOrNull { material in it.members }

    /** @return The species [materials] belongs to, or null if they are not wood. */
    fun detect(materials: List<Material>): WoodFamily? =
        materials.firstNotNullOfOrNull { byMaterial(it) }

    companion object {
        val EMPTY = WoodFamilies(emptyList())
    }
}
