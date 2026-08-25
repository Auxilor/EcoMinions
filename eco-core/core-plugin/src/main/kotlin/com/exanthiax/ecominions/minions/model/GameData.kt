package com.exanthiax.ecominions.minions.model

import org.bukkit.Material

/** The bundled, non-admin-editable game knowledge minions need. */
class GameData(
    val growthRules: GrowthRules,
    /** What a block turns into when auto-smelt is active. */
    private val smelting: Map<Material, Material>
) {
    fun smelt(material: Material): Material? = smelting[material]

    companion object {
        val EMPTY = GameData(GrowthRules.EMPTY, emptyMap())
    }
}
