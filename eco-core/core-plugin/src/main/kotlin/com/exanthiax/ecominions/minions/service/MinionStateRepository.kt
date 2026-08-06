package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.UpgradeCodec
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import java.util.UUID

/**
 * Reads and writes minion state to persistent data containers, whether they
 * belong to a stand or to an item. The only place minion persistence happens.
 */
class MinionStateRepository(
    private val keys: MinionDataKeys,
    private val typeById: (String) -> MinionType?
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
            type = type,
            level = container.get(keys.level, MinionDataKeys.INT) ?: 1,
            owner = container.get(keys.owner, MinionDataKeys.STRING)?.toUuidOrNull(),
            upgrades = UpgradeCodec.decode(container.get(keys.upgrades, MinionDataKeys.STRING) ?: ""),
            inventoryBytes = container.get(keys.inventory, MinionDataKeys.BYTES),
            uuid = container.get(keys.minionUuid, MinionDataKeys.STRING)?.toUuidOrNull(),
            shutdownTimestamp = container.get(keys.shutdownTimestamp, MinionDataKeys.LONG)
        )
    }

    /** Writes everything an item needs to rebuild a minion when placed. */
    fun write(container: PersistentDataContainer, state: MinionState) {
        container.set(keys.isMinion, MinionDataKeys.BYTE, MARKER)
        container.set(keys.type, MinionDataKeys.STRING, state.type.id)
        container.set(keys.level, MinionDataKeys.INT, state.level)
        container.set(keys.upgrades, MinionDataKeys.STRING, UpgradeCodec.encode(state.upgrades))
        state.owner?.let { container.set(keys.owner, MinionDataKeys.STRING, it.toString()) }
        state.uuid?.let { container.set(keys.minionUuid, MinionDataKeys.STRING, it.toString()) }
        state.inventoryBytes?.let { container.set(keys.inventory, MinionDataKeys.BYTES, it) }
        state.shutdownTimestamp?.let { container.set(keys.shutdownTimestamp, MinionDataKeys.LONG, it) }
    }

    /** Persists a live minion, including its storage and burning fuel. */
    fun save(minion: Minion) {
        val container = minion.entity.persistentDataContainer
        val snapshot: Array<ItemStack?> = Array(minion.storage.size) { minion.storage.getItem(it) }

        write(
            container,
            minion.state.copy(
                uuid = minion.uuid,
                inventoryBytes = InventoryCodec.encode(snapshot)
            )
        )

        val fuelId = minion.burningFuelId
        if (fuelId != null && System.currentTimeMillis() < minion.burningFuelEndsAt) {
            container.set(keys.fuelId, MinionDataKeys.STRING, fuelId)
            container.set(keys.fuelEndsAt, MinionDataKeys.LONG, minion.burningFuelEndsAt)
        } else {
            container.remove(keys.fuelId)
            container.remove(keys.fuelEndsAt)
        }
    }

    /** @return The fuel id and expiry stored on [container], if it is still burning. */
    fun readActiveFuel(container: PersistentDataContainer): Pair<String, Long>? {
        val id = container.get(keys.fuelId, MinionDataKeys.STRING) ?: return null
        val endsAt = container.get(keys.fuelEndsAt, MinionDataKeys.LONG) ?: return null
        return if (System.currentTimeMillis() < endsAt) id to endsAt else null
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
