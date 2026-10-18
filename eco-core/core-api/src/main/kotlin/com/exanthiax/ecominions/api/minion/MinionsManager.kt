package com.exanthiax.ecominions.api.minion

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Placed minions, minion items and configured minion types.
 *
 * Every action that changes something fires its cancellable event first. A
 * cancelled action changes nothing and reports [PlaceResult.Status.CANCELLED],
 * [PickUpResult.Status.CANCELLED], 0, false or null, whichever the method
 * returns.
 */
interface MinionsManager {
    /** Every minion currently loaded. Minions in unloaded chunks are not included. */
    fun all(): Collection<Minion>

    fun getByUuid(uuid: UUID): Minion?

    /** @return The minion backed by [entity], or null if it is not a minion. */
    fun getByEntity(entity: Entity): Minion?

    /** @return The loaded minion standing in the block at [location], or null if there is none. */
    fun getByLocation(location: Location): Minion?

    fun ownedBy(player: OfflinePlayer): Collection<Minion>

    /** @return How many loaded minions [player] owns. */
    fun countOwnedBy(player: OfflinePlayer): Int

    /** @return How many minions [player] may place, or [Int.MAX_VALUE] if unlimited. */
    fun placementLimit(player: OfflinePlayer): Int

    fun types(): Collection<MinionDefinition>

    fun getType(id: String): MinionDefinition?

    /**
     * @return Whether [minion] is working. A minion only works while its owner
     *         is online; an idle one is credited when they come back.
     */
    fun isActive(minion: Minion): Boolean

    /**
     * Gives [minion] XP toward its next level, running level-ups as it crosses
     * them. Fires `MinionXpGainEvent`, then `MinionLevelUpEvent` per level gained.
     *
     * Does nothing for a type that cannot level, or one already at its highest level.
     *
     * @return Whether any XP was banked.
     */
    fun giveXp(minion: Minion, amount: Double): Boolean

    /**
     * Sets [minion]'s level directly, clearing its banked XP. Fires no events.
     *
     * @return False if its type cannot level or [level] is outside 1 to its highest level.
     */
    fun setLevel(minion: Minion, level: Int): Boolean

    /**
     * Runs [minion]'s owned compactor recipes over its storage, as the
     * `minion_compact` effect does. Fires `MinionCompactEvent`.
     *
     * @return Whether any recipe ran.
     */
    fun compact(minion: Minion): Boolean

    /** @return Whether [item] is a placeable minion item. */
    fun isMinionItem(item: ItemStack): Boolean

    /** @return The type [item] places, or null if it is not a minion item. */
    fun typeOf(item: ItemStack): MinionDefinition?

    /** @return The level [item] places at, or 0 if it is not a minion item. */
    fun levelOf(item: ItemStack): Int

    /** Builds the placeable item for [type] at level one. It carries no owner until placed. */
    fun createItem(type: MinionDefinition): ItemStack

    /** Builds the placeable item for [type] at [level]. It carries no owner until placed. */
    fun createItem(type: MinionDefinition, level: Int): ItemStack

    /**
     * Gives [player] [amount] items of [type] at [level]. Fires `MinionGiveEvent`.
     *
     * Items that do not fit in the inventory are dropped at the player's feet.
     *
     * @return How many items were given, dropped ones included.
     */
    fun giveItem(player: Player, type: MinionDefinition, level: Int, amount: Int): Int

    /**
     * Takes up to [amount] items of [type] from [player]'s inventory. Fires `MinionTakeEvent`.
     *
     * @return How many were taken, which may be fewer than [amount].
     */
    fun takeItem(player: Player, type: MinionDefinition, amount: Int): Int

    /** @return How many items of [type] are in [player]'s inventory. */
    fun countItems(player: Player, type: MinionDefinition): Int

    /**
     * Places a new minion of [type] at [level], owned by [player], as if they
     * had placed it by hand. Their placement limit applies. Fires `MinionPlaceEvent`.
     */
    fun place(player: Player, type: MinionDefinition, level: Int, location: Location): PlaceResult

    /**
     * Places the minion [item] carries, restoring its level, upgrades and storage.
     *
     * [item] is not consumed. No placement limit applies. Fires `MinionPlaceEvent`
     * with no player.
     *
     * @param owner Who owns the placed minion. Null keeps the owner stored on the item.
     */
    fun place(item: ItemStack, location: Location, owner: OfflinePlayer?): PlaceResult

    /**
     * Picks [minion] up for [player] and gives them its item, as the pickup
     * button does. Fires `MinionPickupEvent` then `MinionDestroyEvent`.
     */
    fun pickUp(player: Player, minion: Minion): PickUpResult

    /**
     * Removes [minion] from the world, deleting its storage. Fires `MinionDestroyEvent`.
     *
     * @return Whether it was removed: false if it is no longer loaded or the event was cancelled.
     */
    fun destroy(minion: Minion, cause: DestroyCause): Boolean

    /**
     * Removes [minion] from the world. Fires `MinionDestroyEvent`.
     *
     * @param dropItem Whether to drop the minion's item, storage included, where it stood.
     * @return Whether it was removed: false if it is no longer loaded or the event was cancelled.
     */
    fun destroy(minion: Minion, cause: DestroyCause, dropItem: Boolean): Boolean

    /** @return How many of [player]'s loaded minions were removed. */
    fun destroyAllOwnedBy(player: OfflinePlayer): Int

    /**
     * Gives [minion] to [player], or makes it unowned if [player] is null.
     * Fires `MinionOwnerChangeEvent`.
     *
     * @return Whether the owner changed.
     */
    fun setOwner(minion: Minion, player: OfflinePlayer?): Boolean

    /** @return A handle on [minion]'s storage. */
    fun storage(minion: Minion): MinionStorage

    /**
     * Sells [minion]'s storage to its owner. Fires `MinionSellEvent`.
     *
     * Only an online owner can be paid, so this does nothing otherwise. Items an
     * active compactor recipe takes are kept back to compact.
     *
     * @return How much was paid: the server economy payout plus what EcoShop paid
     *         in each shop item's own currency.
     */
    fun sell(minion: Minion): Double

    /**
     * Moves [minion]'s storage into [player]'s inventory, as the collect button
     * does. What does not fit stays in storage. Fires `MinionExtractEvent`.
     *
     * @return How many items were moved.
     */
    fun collect(minion: Minion, player: Player): Int

    /**
     * Gives [player] the XP [minion] has stored, as the stored XP button does, and empties the store.
     *
     * @return How much XP was given.
     */
    fun claimExperience(minion: Minion, player: Player): Int
}
