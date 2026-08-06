package com.exanthiax.ecominions.minions.model

import org.bukkit.Material

/**
 * Whether mined blocks regenerate one block higher than they were broken, and
 * what surface they need beneath them when they do.
 */
data class PlacementSettings(
    val liftRegen: Boolean,
    val surfaceBlock: Material?
) {
    companion object {
        val NONE = PlacementSettings(false, null)
    }
}
