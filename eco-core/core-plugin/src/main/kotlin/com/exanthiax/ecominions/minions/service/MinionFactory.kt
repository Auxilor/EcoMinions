package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import org.bukkit.entity.ArmorStand

/** Builds live minions from persisted state, restoring storage and stats. */
class MinionFactory {
    fun create(state: MinionState, entity: ArmorStand): Minion {
        val stats = MinionStats(state.definition.baseStats, state.level)
        val saved = state.inventoryBytes
            ?.let { bytes -> runCatching { InventoryCodec.decode(bytes) }.getOrNull() }
            .orEmpty()
        val storage = MinionStorage(stats.storageSize.coerceAtLeast(saved.size))

        saved.forEachIndexed { index, item ->
            if (item != null) {
                storage.setItem(index, item)
            }
        }

        return Minion(state, entity, storage, stats).apply {
            state.fuel?.let {
                burningFuelId = it.id
                burningFuelRemainingMillis = it.remainingMillis
            }
        }
    }
}
