package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import org.bukkit.entity.ArmorStand

/** Builds live minions from persisted state, restoring storage and stats. */
class MinionFactory {
    fun create(state: MinionState, entity: ArmorStand): Minion {
        val stats = MinionStats(state.type.baseStats)
        val storage = MinionStorage(stats.storageSize)

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
}
