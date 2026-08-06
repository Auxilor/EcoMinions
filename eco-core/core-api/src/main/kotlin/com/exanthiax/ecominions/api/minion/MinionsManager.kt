package com.exanthiax.ecominions.api.minion

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import java.util.UUID

/** Queries over placed minions and configured minion types. */
interface MinionsManager {
    /** Every minion currently loaded. Minions in unloaded chunks are not included. */
    fun all(): Collection<Minion>

    fun getByUuid(uuid: UUID): Minion?

    /** @return The minion backed by [entity], or null if it is not a minion. */
    fun getByEntity(entity: Entity): Minion?

    fun ownedBy(player: OfflinePlayer): Collection<Minion>

    /** @return How many loaded minions [player] owns. */
    fun countOwnedBy(player: OfflinePlayer): Int

    /** @return How many minions [player] may place, or [Int.MAX_VALUE] if unlimited. */
    fun placementLimit(player: OfflinePlayer): Int

    fun types(): Collection<MinionType>

    fun getType(id: String): MinionType?

    /**
     * Builds the placeable item for [type] at [level].
     *
     * @return A new item stack; it carries no owner until placed.
     */
    fun createItem(type: MinionType, level: Int = 1): ItemStack
}
