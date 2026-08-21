package com.exanthiax.ecominions.api.minion

import org.bukkit.Material

/** A configured kind of minion, e.g. `stone` or `wheat`. */
interface MinionType {
    /** Config file name, without extension. */
    val id: String

    /** Formatted display name. */
    val name: String

    /** Blocks this type mines. */
    val targetBlocks: Set<Material>

    /** Ticks between mining actions before upgrades are applied. */
    val baseSpeedTicks: Int

    /** Storage slots before upgrades are applied. */
    val baseStorageSlots: Int

    /** Ids of the upgrades this type is allowed to buy. */
    val allowedUpgradeIds: List<String>

    /** Whether this type grows and fells trees rather than mining a fixed block set. */
    val isTreeType: Boolean
}
