package com.exanthiax.ecominions.minions.model

import org.bukkit.Material

/** The bundled, non-admin-editable game knowledge minions need. */
class GameData(
    val growthRules: GrowthRules,
    val woodFamilies: WoodFamilies,
    /** Which block a crop needs beneath it, e.g. wheat on farmland. */
    private val cropSurfaces: Map<Material, Material>,
    /** What a block turns into when auto-smelt is active. */
    private val smelting: Map<Material, Material>
) {
    fun surfaceForCrop(material: Material): Material? = cropSurfaces[material]

    fun anyCropSurfaceIn(materials: Iterable<Material>): Material? =
        materials.firstNotNullOfOrNull { surfaceForCrop(it) }

    fun smelt(material: Material): Material? = smelting[material]

    companion object {
        val EMPTY = GameData(GrowthRules.EMPTY, WoodFamilies.EMPTY, emptyMap(), emptyMap())
    }
}
