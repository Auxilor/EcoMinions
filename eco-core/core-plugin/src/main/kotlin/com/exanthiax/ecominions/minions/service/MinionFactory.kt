package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand

/** Builds live minions from persisted state, restoring storage and stats. */
class MinionFactory {
    fun create(state: MinionState, entity: ArmorStand): Minion {
        val stats = MinionStats(state.type.baseStats, state.level)
        val storage = Bukkit.createInventory(null, stats.storageSize, STORAGE_TITLE)

        state.inventoryBytes?.let { bytes ->
            runCatching { InventoryCodec.decode(bytes) }
                .getOrNull()
                ?.forEachIndexed { index, item ->
                    if (index < storage.size && item != null) {
                        storage.setItem(index, item)
                    }
                }
        }

        return Minion(state, entity, storage, stats)
    }

    private companion object {
        const val STORAGE_TITLE = "Minion"
    }
}
