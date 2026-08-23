package com.exanthiax.ecominions.minions.model

/** Equipment worn/held by a minion's stand, each slot an eco item-lookup string. */
data class MinionEquipment(
    val helmet: String?,
    val chestplate: String?,
    val leggings: String?,
    val boots: String?,
    val hand: String?
) {
    companion object {
        val EMPTY = MinionEquipment(null, null, null, null, null)
    }
}
