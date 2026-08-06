package com.exanthiax.ecominions.minions.model

import org.bukkit.Color
import org.bukkit.Material

/** Armour worn by a minion's stand. */
data class MinionArmor(
    val helmet: MinionArmorSlot?,
    val chestplate: MinionArmorSlot?,
    val leggings: MinionArmorSlot?,
    val boots: MinionArmorSlot?
) {
    companion object {
        val EMPTY = MinionArmor(null, null, null, null)
    }
}

/** One armour piece, optionally dyed. */
data class MinionArmorSlot(
    val material: Material,
    val color: Color?
)
