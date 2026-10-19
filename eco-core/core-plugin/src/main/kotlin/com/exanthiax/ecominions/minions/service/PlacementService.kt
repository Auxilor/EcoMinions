package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionDestroyEvent
import com.exanthiax.ecominions.api.event.MinionPickupEvent
import com.exanthiax.ecominions.api.event.MinionPlaceEvent
import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.islands.IslandLimits
import com.exanthiax.ecominions.islands.IslandRegistry
import com.exanthiax.ecominions.islands.MinionAccess
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionPickup
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionPlace
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.willfp.eco.core.items.Items
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/** Places minions in the world and takes them back out of it. */
class PlacementService(
    private val minions: MinionService,
    private val factory: MinionFactory,
    private val itemFactory: MinionItemFactory,
    private val repository: MinionStateRepository,
    private val limits: PlacementLimits,
    private val worlds: WorldSettings,
    private val settings: SettingsHolder<MinionSettings>,
    private val triggers: MinionTriggerDispatch,
    private val events: EventDispatcher,
    private val access: MinionAccess,
    private val islands: IslandRegistry,
    private val islandLimits: IslandLimits,
    private val canPlace: (Player, Block) -> Boolean
) {
    sealed interface PlaceResult {
        data class Placed(val minion: Minion) : PlaceResult
        data object WorldDisabled : PlaceResult
        data class LimitReached(val limit: Int) : PlaceResult
        data class IslandLimitReached(val limit: Int) : PlaceResult
        data object Protected : PlaceResult
        data object NoIslandPermission : PlaceResult
        data object Cancelled : PlaceResult
    }

    sealed interface PickUpResult {
        data class PickedUp(val item: ItemStack) : PickUpResult
        data object NotOwner : PickUpResult
        data object NoIslandPermission : PickUpResult
        data object Cancelled : PickUpResult
    }

    fun place(player: Player, state: MinionState, location: Location): PlaceResult {
        if (isDisabled(location)) {
            return PlaceResult.WorldDisabled
        }
        if (!state.definition.bypassAntigrief && !canPlace(player, location.block)) {
            return PlaceResult.Protected
        }
        val island by lazy { islands.islandAt(location) }
        if (!access.canPlace(player) { island }) {
            return PlaceResult.NoIslandPermission
        }
        val limit = limits.limitFor(player)
        if (minions.countOwnedBy(player) >= limit) {
            return PlaceResult.LimitReached(limit)
        }
        if (settings.value.islandLimitsEnabled && !player.hasPermission(ADMIN_PERMISSION)) {
            island?.takeIf { islandLimits.isFull(it) }?.let {
                return PlaceResult.IslandLimitReached(islandLimits.limitFor(it))
            }
        }

        val owner = state.owner?.takeIf { it != player.uniqueId }?.let { Bukkit.getOfflinePlayer(it) } ?: player
        return placeAs(player, state, location.facing(player), owner)
    }

    /**
     * Places [state] with no player involved, so no placement limit applies.
     *
     * @param owner Who owns the minion. Null keeps whoever [state] names.
     */
    fun placeUnattended(state: MinionState, location: Location, owner: OfflinePlayer?): PlaceResult =
        placeAs(null, state, location, owner ?: state.owner?.let { Bukkit.getOfflinePlayer(it) })

    private fun placeAs(player: Player?, state: MinionState, location: Location, owner: OfflinePlayer?): PlaceResult {
        if (isDisabled(location)) {
            return PlaceResult.WorldDisabled
        }

        val event = MinionPlaceEvent(player, state.definition, state.level, location, owner)
        if (!events.dispatch(event)) {
            return PlaceResult.Cancelled
        }
        if (isDisabled(event.location)) {
            return PlaceResult.WorldDisabled
        }

        val placed = state.copy(
            owner = event.owner?.uniqueId,
            uuid = state.uuid ?: UUID.randomUUID()
        )
        val minion = spawn(placed, event.location)
        triggers.trigger(minion, TriggerMinionPlace)
        return PlaceResult.Placed(minion)
    }

    private fun isDisabled(location: Location): Boolean {
        val world = location.world ?: return true
        return worlds.isDisabled(world)
    }

    /** Removes [minion] and hands back the item that carries its contents. */
    fun pickUp(player: Player, minion: Minion): PickUpResult {
        if (!access.canPickUp(player, minion)) {
            return if (access.deniedByIsland(player, minion, IslandPrivilege.PICKUP)) {
                PickUpResult.NoIslandPermission
            } else {
                PickUpResult.NotOwner
            }
        }

        val event = MinionPickupEvent(player, minion, itemFor(minion))
        if (!events.dispatch(event) || !events.dispatch(MinionDestroyEvent(minion, DestroyCause.PICKUP))) {
            return PickUpResult.Cancelled
        }

        remove(minion)
        triggers.trigger(minion, TriggerMinionPickup)
        return PickUpResult.PickedUp(event.item)
    }

    /**
     * Removes [minion] from the world.
     *
     * @param dropItem Whether to drop the minion's item, storage included, where it stood.
     * @return Whether it was removed.
     */
    fun destroy(minion: Minion, cause: DestroyCause, dropItem: Boolean): Boolean {
        if (!events.dispatch(MinionDestroyEvent(minion, cause))) {
            return false
        }

        val location = minion.entity.location
        val item = if (dropItem) itemFor(minion) else null
        remove(minion)
        item?.let { location.world?.dropItemNaturally(location, it) }
        return true
    }

    /** The item that places [minion] again with everything it has, storage included. */
    fun itemFor(minion: Minion): ItemStack =
        itemFactory.build(
            minion.state.copy(
                inventoryBytes = InventoryCodec.encode(minion.storage.snapshot()),
                fuel = repository.fuelOf(minion)
            )
        )

    /** A slayer's frozen mobs would stand forever without it. */
    private fun removeSpawnedMobs(minion: Minion) {
        for (uuid in minion.mining.spawnedMobs.toList()) {
            minion.entity.world.getEntity(uuid)?.remove()
        }
        minion.mining.spawnedMobs.clear()
    }

    private fun remove(minion: Minion) {
        clearRegenMarkers(minion)
        if (settings.value.clearSeededOnPickup) {
            clearSeeded(minion)
        }
        removeSpawnedMobs(minion)
        minions.retire(minion)
        minion.entity.remove()
    }

    /** Minions look back at whoever placed them, squared to the nearest cardinal direction. */
    private fun Location.facing(player: Player): Location = clone().apply {
        yaw = MinionFacing.towards(player.location.yaw)
        pitch = 0.0f
    }

    private fun spawn(state: MinionState, location: Location): Minion {
        val world = location.world ?: error("Cannot spawn a minion in a null world")
        val stand = world.spawn(location, ArmorStand::class.java) { stand ->
            dressStand(stand, state)
            repository.markAsMinion(stand.persistentDataContainer)
        }

        val minion = factory.create(state.copy(uuid = state.uuid ?: stand.uniqueId), stand)
        if (state.shutdownTimestamp == null) {
            minion.thawFuel(System.currentTimeMillis())
        }
        repository.save(minion)
        val registered = minions.register(minion)
        minions.refreshEffects(registered)
        return registered
    }

    private fun dressStand(stand: ArmorStand, state: MinionState) {
        stand.isSmall = true
        stand.isVisible = true
        stand.setGravity(false)
        stand.isInvulnerable = settings.value.invulnerable
        stand.isPersistent = true
        stand.setBasePlate(false)
        stand.setArms(true)
        stand.setMarker(false)
        stand.removeWhenFarAway = false

        val configured = state.definition.equipment
        val equipment = stand.equipment
        equipment.helmet = configured.helmet?.toItemStack() ?: itemFactory.headFor(state.definition)
        configured.chestplate?.toItemStack()?.let { equipment.chestplate = it }
        configured.leggings?.toItemStack()?.let { equipment.leggings = it }
        configured.boots?.toItemStack()?.let { equipment.boots = it }
        equipment.setItemInMainHand(configured.hand?.toItemStack() ?: ItemStack(Material.IRON_PICKAXE))
    }

    /** Regen markers are real blocks; leaving them behind would grief the world. */
    private fun clearRegenMarkers(minion: Minion) {
        val fallback = settings.value.regenMarkerFallback
        for (location in minion.mining.pendingRegen) {
            val block = location.block
            if (block.type == RegenService.MARKER_BLOCK) {
                block.type = fallback
            }
        }
        minion.mining.pendingRegen.clear()
    }

    /** Clears what [minion] placed, leaving any spot a player has since built on. */
    private fun clearSeeded(minion: Minion) {
        val definition = minion.state.definition
        val own = definition.placement.seedBlocks +
            definition.targetBlocks.definitions.flatMap { listOfNotNull(it.block, it.plant, it.leaves) }
        for (location in minion.mining.seeded) {
            val block = location.block
            if (block.type == Material.WATER || block.type == RegenService.MARKER_BLOCK || own.any { it.matches(block) }) {
                block.type = Material.AIR
            }
        }
        minion.mining.seeded.clear()
    }

    private fun String.toItemStack(): ItemStack = Items.lookup(this).item.clone()

    private companion object {
        const val ADMIN_PERMISSION = "ecominions.admin"
    }
}
