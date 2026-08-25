package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionDefinition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * What each of a type's target blocks actually drops.
 *
 * Offline catch-up used to credit the ore block itself, which diverged from live
 * mining and left the drops unsellable. Resolution is cached because a loot table
 * lookup per simulated cycle would be far too expensive on the login tick.
 *
 * [resolve] is injected so the cache can be tested without a server.
 */
class MinionDropTables(
    private val resolve: (Material, Location) -> List<ItemStack>
) {
    private val cache = HashMap<Key, List<ItemStack>>()

    fun dropsFor(type: MinionDefinition, material: Material, location: Location): List<ItemStack> {
        val cached = cache.getOrPut(Key(type.id, material)) {
            resolve(material, location).ifEmpty { listOf(ItemStack(material, 1)) }
        }

        return cached.map { it.clone() }
    }

    fun clear() {
        cache.clear()
    }

    private data class Key(val typeId: String, val material: Material)
}
