package com.exanthiax.ecominions.api.minion

import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.UUID

/** A minion placed in the world. */
interface Minion {
    /** Stable id, preserved across pickup and re-placement. */
    val uuid: UUID

    val type: MinionDefinition

    /**
     * The minion's level.
     *
     * XP-driven when the type is levellable; otherwise the sum of purchased
     * upgrade tiers, plus one.
     */
    val level: Int

    /** XP banked toward the next level. Always zero for a type that cannot level. */
    val xp: Double

    /** Owning player, or null if the minion was placed unowned. */
    val owner: UUID?

    val location: Location

    /** The stand that shows this minion in the world. */
    val entity: Entity

    /** Purchased upgrade tiers, keyed by upgrade id. */
    val upgradeLevels: Map<String, Int>

    /** Slots in the minion's storage. */
    val storageSize: Int

    /** Slots currently holding an item. */
    val usedStorage: Int

    val isStorageFull: Boolean

    /** Ticks between mining actions, with upgrades and effects applied but not fuel. */
    val speedTicks: Int

    /** Ticks a single break takes, with upgrades and effects applied. */
    val efficiencyTicks: Int

    /** How far out from itself the minion works, in blocks. */
    val radius: Int

    /** How many blocks tall the minion's work area is. */
    val height: Int

    /** Ticks a broken block takes to grow back. */
    val regenDelayTicks: Int
}
