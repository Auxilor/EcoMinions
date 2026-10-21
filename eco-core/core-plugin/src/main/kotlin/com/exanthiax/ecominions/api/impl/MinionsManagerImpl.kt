package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.event.MinionOwnerChangeEvent
import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.api.minion.MinionStorage
import com.exanthiax.ecominions.api.minion.MinionsManager
import com.exanthiax.ecominions.api.minion.PickUpResult
import com.exanthiax.ecominions.api.minion.PlaceResult
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.islands.IslandLimits
import com.exanthiax.ecominions.islands.MinionAccess
import com.exanthiax.ecominions.levelling.service.MinionXpService
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionCompactService
import com.exanthiax.ecominions.minions.service.MinionCollectService
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionItemService
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.minions.service.SellService
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion
import com.exanthiax.ecominions.api.minion.MinionDefinition as ApiMinionDefinition

/**
 * The public minions API, over the internal minion services.
 *
 * Calls that change a placed minion answer synchronously, so on Folia they run
 * where they are called and log once when that is not the minion's region.
 */
class MinionsManagerImpl(
    private val minions: MinionService,
    private val placement: PlacementService,
    private val itemFactory: MinionItemFactory,
    private val items: MinionItemService,
    private val collector: MinionCollectService,
    private val selling: SellService,
    private val repository: MinionStateRepository,
    private val activity: MinionActivityService,
    private val xp: MinionXpService,
    private val compacting: MinionCompactService,
    private val events: EventDispatcher,
    private val regions: Regions,
    private val access: MinionAccess,
    private val islandLimits: IslandLimits,
    private val ownerOf: (UUID) -> OfflinePlayer
) : MinionsManager {

    override fun all(): Collection<ApiMinion> = minions.all()

    override fun islandLimit(island: MinionIsland): Int = islandLimits.limitFor(island)

    override fun canUse(player: Player, minion: ApiMinion): Boolean =
        access.canUse(player, resolve(minion))

    override fun canPickUp(player: Player, minion: ApiMinion): Boolean =
        access.canPickUp(player, resolve(minion))

    override fun getByUuid(uuid: UUID): ApiMinion? = minions.getByUuid(uuid)

    override fun getByEntity(entity: Entity): ApiMinion? = minions.getByEntity(entity)

    override fun getByLocation(location: Location): ApiMinion? =
        minions.all().firstOrNull { minion ->
            val standing = minion.entity.location
            standing.world == location.world &&
                standing.blockX == location.blockX &&
                standing.blockY == location.blockY &&
                standing.blockZ == location.blockZ
        }

    override fun ownedBy(player: OfflinePlayer): Collection<ApiMinion> = minions.ownedBy(player)

    override fun countOwnedBy(player: OfflinePlayer): Int = minions.countOwnedBy(player)

    override fun placementLimit(player: OfflinePlayer): Int = minions.placementLimit(player)

    override fun types(): Collection<ApiMinionDefinition> = minions.types()

    override fun getType(id: String): ApiMinionDefinition? = minions.getType(id)

    override fun isActive(minion: ApiMinion): Boolean = activity.isActive(resolve(minion))

    override fun giveXp(minion: ApiMinion, amount: Double): Boolean = xp.grant(owned(minion), amount)

    override fun setLevel(minion: ApiMinion, level: Int): Boolean = xp.setLevel(owned(minion), level)

    override fun compact(minion: ApiMinion): Boolean = compacting.compact(owned(minion))

    override fun isMinionItem(item: ItemStack): Boolean = itemFactory.isMinionItem(item)

    override fun typeOf(item: ItemStack): ApiMinionDefinition? = itemFactory.read(item)?.definition

    override fun levelOf(item: ItemStack): Int = itemFactory.read(item)?.level ?: 0

    override fun createItem(type: ApiMinionDefinition): ItemStack = createItem(type, 1)

    override fun createItem(type: ApiMinionDefinition, level: Int): ItemStack =
        itemFactory.build(MinionState(definition = resolve(type), storedLevel = level))

    override fun giveItem(player: Player, type: ApiMinionDefinition, level: Int, amount: Int): Int =
        items.give(player, resolve(type), level, amount)

    override fun takeItem(player: Player, type: ApiMinionDefinition, amount: Int): Int =
        items.take(player, resolve(type), amount)

    override fun countItems(player: Player, type: ApiMinionDefinition): Int = items.count(player, resolve(type))

    override fun place(player: Player, type: ApiMinionDefinition, level: Int, location: Location): PlaceResult {
        regions.warnIfNotOwned(location, PLACING)
        return placement.place(player, MinionState(definition = resolve(type), storedLevel = level), location).toPublic()
    }

    override fun place(item: ItemStack, location: Location, owner: OfflinePlayer?): PlaceResult {
        regions.warnIfNotOwned(location, PLACING)
        val state = itemFactory.read(item) ?: return PlaceResult(PlaceResult.Status.INVALID_ITEM, null, 0)
        return placement.placeUnattended(state, location, owner).toPublic()
    }

    override fun pickUp(player: Player, minion: ApiMinion): PickUpResult =
        when (val result = placement.pickUp(player, owned(minion))) {
            is PlacementService.PickUpResult.PickedUp -> {
                for (leftover in player.inventory.addItem(result.item).values) {
                    player.world.dropItemNaturally(player.location, leftover)
                }
                PickUpResult(PickUpResult.Status.PICKED_UP, result.item)
            }

            PlacementService.PickUpResult.NotOwner -> PickUpResult(PickUpResult.Status.NOT_OWNER, null)
            PlacementService.PickUpResult.NoIslandPermission ->
                PickUpResult(PickUpResult.Status.NO_ISLAND_PERMISSION, null)
            PlacementService.PickUpResult.Cancelled -> PickUpResult(PickUpResult.Status.CANCELLED, null)
        }

    override fun destroy(minion: ApiMinion, cause: DestroyCause): Boolean = destroy(minion, cause, false)

    override fun destroy(minion: ApiMinion, cause: DestroyCause, dropItem: Boolean): Boolean {
        if (minions.getByUuid(minion.uuid) == null) {
            return false
        }
        return placement.destroy(owned(minion), cause, dropItem)
    }

    override fun destroyAllOwnedBy(player: OfflinePlayer): Int = destroyAllOwnedBy(player, DestroyCause.API)

    /** As [destroyAllOwnedBy], naming why. */
    fun destroyAllOwnedBy(player: OfflinePlayer, cause: DestroyCause): Int =
        minions.ownedMinions(player.uniqueId).count {
            regions.warnIfNotOwned(it.entity, CHANGING)
            placement.destroy(it, cause, false)
        }

    override fun setOwner(minion: ApiMinion, player: OfflinePlayer?): Boolean {
        val resolved = owned(minion)
        val event = MinionOwnerChangeEvent(resolved, resolved.state.owner?.let(ownerOf), player)
        if (!events.dispatch(event)) {
            return false
        }
        minions.changeOwner(resolved, event.newOwner?.uniqueId)
        return true
    }

    override fun storage(minion: ApiMinion): MinionStorage = MinionStorageImpl(resolve(minion), repository)

    override fun sell(minion: ApiMinion): Double = selling.sellStorage(owned(minion))

    override fun collect(minion: ApiMinion, player: Player): Int = collector.collect(player, owned(minion)) ?: 0

    override fun claimExperience(minion: ApiMinion, player: Player): Int =
        collector.claimExperience(player, owned(minion))

    /** As [resolve], logging once on Folia if the caller is not on the minion's region. */
    private fun owned(minion: ApiMinion): Minion =
        resolve(minion).also { regions.warnIfNotOwned(it.entity, CHANGING) }

    private fun resolve(minion: ApiMinion): Minion =
        minions.getByUuid(minion.uuid)
            ?: throw IllegalArgumentException("Minion ${minion.uuid} is not loaded")

    private fun resolve(type: ApiMinionDefinition): MinionDefinition =
        minions.getType(type.id)
            ?: throw IllegalArgumentException("Unknown minion: ${type.id}")

    private fun PlacementService.PlaceResult.toPublic(): PlaceResult = when (this) {
        is PlacementService.PlaceResult.Placed -> PlaceResult(PlaceResult.Status.PLACED, minion, 0)
        PlacementService.PlaceResult.WorldDisabled -> PlaceResult(PlaceResult.Status.WORLD_DISABLED, null, 0)
        is PlacementService.PlaceResult.LimitReached -> PlaceResult(PlaceResult.Status.LIMIT_REACHED, null, limit)
        is PlacementService.PlaceResult.IslandLimitReached ->
            PlaceResult(PlaceResult.Status.ISLAND_LIMIT_REACHED, null, limit)
        is PlacementService.PlaceResult.TypeLimitReached -> PlaceResult(PlaceResult.Status.TYPE_LIMIT_REACHED, null, limit)
        is PlacementService.PlaceResult.MinionLimitReached ->
            PlaceResult(PlaceResult.Status.MINION_LIMIT_REACHED, null, limit)
        PlacementService.PlaceResult.Protected -> PlaceResult(PlaceResult.Status.PROTECTED, null, 0)
        PlacementService.PlaceResult.NoIslandPermission ->
            PlaceResult(PlaceResult.Status.NO_ISLAND_PERMISSION, null, 0)
        PlacementService.PlaceResult.Cancelled -> PlaceResult(PlaceResult.Status.CANCELLED, null, 0)
    }

    private companion object {
        const val CHANGING = "Changing a minion through the EcoMinions API"
        const val PLACING = "Placing a minion through the EcoMinions API"
    }
}
