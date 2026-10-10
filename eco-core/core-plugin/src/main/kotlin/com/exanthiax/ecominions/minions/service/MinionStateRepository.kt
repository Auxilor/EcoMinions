package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.StoredFuel
import com.exanthiax.ecominions.minions.model.UpgradeCodec
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import java.util.UUID

/**
 * Reads and writes minion state to persistent data containers, whether they
 * belong to a stand or to an item. The only place minion persistence happens.
 */
class MinionStateRepository(
    private val keys: MinionDataKeys,
    private val typeById: (String) -> MinionDefinition?
) {
    fun isMinion(container: PersistentDataContainer): Boolean =
        container.get(keys.isMinion, MinionDataKeys.BYTE) == MARKER

    fun read(container: PersistentDataContainer): MinionState? {
        if (!isMinion(container)) {
            return null
        }
        val typeId = container.get(keys.type, MinionDataKeys.STRING) ?: return null
        val type = typeById(typeId) ?: return null

        return MinionState(
            definition = type,
            storedLevel = container.get(keys.level, MinionDataKeys.INT) ?: 1,
            xp = container.get(keys.xp, MinionDataKeys.DOUBLE) ?: 0.0,
            owner = container.get(keys.owner, MinionDataKeys.STRING)?.toUuidOrNull(),
            upgrades = UpgradeCodec.decode(container.get(keys.upgrades, MinionDataKeys.STRING) ?: ""),
            inventoryBytes = container.get(keys.inventory, MinionDataKeys.BYTES),
            uuid = container.get(keys.minionUuid, MinionDataKeys.STRING)?.toUuidOrNull(),
            shutdownTimestamp = container.get(keys.shutdownTimestamp, MinionDataKeys.LONG),
            fuel = readStoredFuel(container)
        )
    }

    /** Writes everything an item needs to rebuild a minion when placed. */
    fun write(container: PersistentDataContainer, state: MinionState) {
        container.set(keys.isMinion, MinionDataKeys.BYTE, MARKER)
        container.set(keys.type, MinionDataKeys.STRING, state.definition.id)
        container.set(keys.level, MinionDataKeys.INT, state.level)
        container.set(keys.xp, MinionDataKeys.DOUBLE, state.xp)
        container.set(keys.upgrades, MinionDataKeys.STRING, UpgradeCodec.encode(state.upgrades))
        state.owner?.let { container.set(keys.owner, MinionDataKeys.STRING, it.toString()) }
        state.uuid?.let { container.set(keys.minionUuid, MinionDataKeys.STRING, it.toString()) }
        state.inventoryBytes?.let { container.set(keys.inventory, MinionDataKeys.BYTES, it) }
        state.shutdownTimestamp?.let { container.set(keys.shutdownTimestamp, MinionDataKeys.LONG, it) }

        val fuel = state.fuel
        if (fuel != null && fuel.remainingMillis > 0L) {
            container.set(keys.fuelId, MinionDataKeys.STRING, fuel.id)
            container.set(keys.fuelRemaining, MinionDataKeys.LONG, fuel.remainingMillis)
        } else {
            container.remove(keys.fuelId)
            container.remove(keys.fuelRemaining)
        }
    }

    /** Persists a live minion, including its storage and burning fuel. */
    fun save(minion: Minion) {
        val container = minion.entity.persistentDataContainer
        val snapshot: Array<ItemStack?> = minion.storage.snapshot()

        write(
            container,
            minion.state.copy(
                uuid = minion.uuid,
                inventoryBytes = InventoryCodec.encode(snapshot),
                fuel = fuelOf(minion)
            )
        )
        container.set(keys.seeded, MinionDataKeys.LONGS, minion.mining.seeded.map(BlockPositions::pack).toLongArray())
    }

    /** @return The positions [stand]'s minion placed blocks at, as last saved. */
    fun readSeeded(stand: ArmorStand): List<Location> =
        stand.persistentDataContainer.get(keys.seeded, MinionDataKeys.LONGS)
            ?.map { BlockPositions.unpack(stand.world, it) }
            .orEmpty()

    /** @return The fuel [minion] is burning and the time left on it, if any. */
    fun fuelOf(minion: Minion): StoredFuel? {
        val id = minion.burningFuelId ?: return null
        val remaining = if (minion.burningFuelEndsAt > 0L) {
            (minion.burningFuelEndsAt - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            minion.burningFuelRemainingMillis
        }
        return StoredFuel(id, remaining).takeIf { remaining > 0L }
    }

    private fun readStoredFuel(container: PersistentDataContainer): StoredFuel? {
        val id = container.get(keys.fuelId, MinionDataKeys.STRING) ?: return null
        val remaining = container.get(keys.fuelRemaining, MinionDataKeys.LONG) ?: return null
        return StoredFuel(id, remaining).takeIf { remaining > 0L }
    }

    fun markShutdown(minion: Minion, timestamp: Long) {
        minion.entity.persistentDataContainer
            .set(keys.shutdownTimestamp, MinionDataKeys.LONG, timestamp)
    }

    fun clearShutdown(minion: Minion) {
        minion.entity.persistentDataContainer.remove(keys.shutdownTimestamp)
    }

    /** Marks a freshly spawned stand before its state has been written. */
    fun markAsMinion(container: PersistentDataContainer) {
        container.set(keys.isMinion, MinionDataKeys.BYTE, MARKER)
    }

    private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private companion object {
        const val MARKER: Byte = 1
    }
}
