package com.exanthiax.ecominions.api.minion

import org.bukkit.Material

/** A configured minion, e.g. `stone` or `wheat`. */
interface MinionDefinition {
    /** Config file name, without extension. */
    val id: String

    /** Formatted display name. */
    val name: String

    /**
     * Ids of the blocks this minion mines, as written in its config.
     *
     * Ids rather than materials because a target may be a custom block from
     * ItemsAdder, Oraxen, Nexo or CraftEngine, which no [Material] names - and
     * because this module deliberately depends on nothing but Paper.
     */
    val targetBlockIds: Set<String>

    /** What this minion does with the blocks it breaks. */
    val type: MinionType

    /** Ticks between mining actions before upgrades are applied. */
    val baseSpeedTicks: Int

    /** Storage slots before upgrades are applied. */
    val baseStorageSlots: Int

    /** Ids of the upgrades this type is allowed to buy. */
    val allowedUpgradeIds: List<String>

    /** Whether this type levels from XP, rather than from purchased upgrade tiers. */
    val isLevellable: Boolean

    /** The highest level this type can reach, or 1 if it cannot level. */
    val maxLevel: Int
}
