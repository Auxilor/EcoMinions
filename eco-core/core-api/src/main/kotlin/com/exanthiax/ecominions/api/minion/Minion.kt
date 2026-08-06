package com.exanthiax.ecominions.api.minion

import org.bukkit.Location
import java.util.UUID

/** A minion placed in the world. */
interface Minion {
    /** Stable id, preserved across pickup and re-placement. */
    val uuid: UUID

    val type: MinionType

    /** Sum of purchased upgrade tiers, plus one. */
    val level: Int

    /** Owning player, or null if the minion was placed unowned. */
    val owner: UUID?

    val location: Location

    /** Purchased upgrade tiers, keyed by upgrade id. */
    val upgradeLevels: Map<String, Int>

    /** Slots in the minion's storage. */
    val storageSize: Int

    /** Slots currently holding an item. */
    val usedStorage: Int
}
