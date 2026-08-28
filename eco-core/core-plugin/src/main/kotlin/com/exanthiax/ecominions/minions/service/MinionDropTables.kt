package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.MinionDefinition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * What each of a minion's target blocks actually drops.
 *
 * Offline catch-up used to credit the ore block itself, which diverged from live
 * mining and left the drops unsellable. Resolution is cached because a loot table
 * lookup per simulated cycle would be far too expensive on the login tick.
 *
 * Both resolvers are injected so the cache can be tested without a server.
 */
class MinionDropTables(
    /** Reads a vanilla block's loot table. */
    private val resolve: (Material, Location) -> List<ItemStack>,
    /** Resolves one id from a definition's `drops:`, or null if this server has no such item. */
    private val resolveItem: (String) -> ItemStack?
) {
    private val cache = HashMap<Key, List<ItemStack>>()

    /**
     * @return What one [block] is worth to [definition] while nobody is online.
     *
     * A custom block's loot table cannot be read offline, which is what its
     * `drops:` list is for. A custom block with no `drops:` credits nothing:
     * there is no correct answer, and guessing an item is worse than none.
     */
    fun dropsFor(definition: MinionDefinition, block: MinionBlock, location: Location): List<ItemStack> {
        val cached = cache.getOrPut(Key(definition.id, block.id)) {
            when {
                block.drops.isNotEmpty() -> block.drops.mapNotNull(resolveItem)
                block.vanillaMaterial != null -> resolve(block.vanillaMaterial, location)
                    .ifEmpty { listOf(ItemStack(block.vanillaMaterial, 1)) }

                else -> emptyList()
            }
        }

        return cached.map { it.clone() }
    }

    fun clear() {
        cache.clear()
    }

    private data class Key(val definitionId: String, val blockId: String)
}
