package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionPickupEvent
import com.exanthiax.ecominions.api.event.MinionPlaceEvent
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.minions.model.MinionArmorSlot
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import java.util.UUID

/** Places minions in the world and takes them back out of it. */
class PlacementService(
    private val minions: MinionService,
    private val factory: MinionFactory,
    private val itemFactory: MinionItemFactory,
    private val repository: MinionStateRepository,
    private val limits: PlacementLimits,
    private val worlds: WorldSettings,
    private val settings: SettingsHolder<MinionSettings>
) {
    sealed interface PlaceResult {
        data class Placed(val minion: Minion) : PlaceResult
        data object WorldDisabled : PlaceResult
        data class LimitReached(val limit: Int) : PlaceResult
    }

    fun place(player: Player, state: MinionState, location: Location): PlaceResult {
        val world = location.world ?: return PlaceResult.WorldDisabled
        if (worlds.isDisabled(world)) {
            return PlaceResult.WorldDisabled
        }
        if (!player.hasPermission(ADMIN_PERMISSION) &&
            minions.countOwnedBy(player) >= limits.limitFor(player)
        ) {
            return PlaceResult.LimitReached(limits.limitFor(player))
        }

        val owned = state.copy(
            owner = state.owner ?: player.uniqueId,
            uuid = state.uuid ?: UUID.randomUUID()
        )
        val minion = spawn(owned, location)

        Bukkit.getPluginManager().callEvent(MinionPlaceEvent(minion, player))
        return PlaceResult.Placed(minion)
    }

    /**
     * Removes [minion] and returns the item that carries its contents.
     *
     * @return The item to give the player, or null if they may not pick it up.
     */
    fun pickUp(player: Player, minion: Minion): ItemStack? {
        if (!canPickUp(player, minion)) {
            return null
        }

        val snapshot: Array<ItemStack?> = Array(minion.storage.size) { minion.storage.getItem(it) }
        val item = itemFactory.build(minion.state.copy(inventoryBytes = InventoryCodec.encode(snapshot)))

        clearRegenMarkers(minion)
        minions.unregister(minion)
        minion.entity.remove()

        Bukkit.getPluginManager().callEvent(MinionPickupEvent(minion, player))
        return item
    }

    fun canPickUp(player: Player, minion: Minion): Boolean =
        minion.state.owner == null ||
            minion.state.owner == player.uniqueId ||
            player.hasPermission(ADMIN_PERMISSION)

    private fun spawn(state: MinionState, location: Location): Minion {
        val world = location.world ?: error("Cannot spawn a minion in a null world")
        val stand = world.spawn(location, ArmorStand::class.java) { stand ->
            dressStand(stand, state)
            repository.markAsMinion(stand.persistentDataContainer)
        }

        val minion = factory.create(state.copy(uuid = state.uuid ?: stand.uniqueId), stand)
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

        val armor = state.type.armor
        val equipment = stand.equipment
        equipment.helmet = armor.helmet?.toItemStack() ?: itemFactory.headFor(state.type)
        armor.chestplate?.let { equipment.chestplate = it.toItemStack() }
        armor.leggings?.let { equipment.leggings = it.toItemStack() }
        armor.boots?.let { equipment.boots = it.toItemStack() }
        equipment.setItemInMainHand(ItemStack(Material.IRON_PICKAXE))
    }

    /** Regen markers are real bedrock; leaving them behind would grief the world. */
    private fun clearRegenMarkers(minion: Minion) {
        val fallback = settings.value.regenMarkerFallback
        for (location in minion.mining.pendingRegen) {
            val block = location.block
            if (block.type == Material.BEDROCK) {
                block.type = fallback
            }
        }
        minion.mining.pendingRegen.clear()
    }

    private fun MinionArmorSlot.toItemStack(): ItemStack {
        val item = ItemStack(material)
        val color = color ?: return item
        val meta = item.itemMeta as? LeatherArmorMeta ?: return item
        meta.setColor(color)
        item.itemMeta = meta
        return item
    }

    private companion object {
        const val ADMIN_PERMISSION = "ecominions.admin"
    }
}
