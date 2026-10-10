package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.MinionDefinition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * What each of a minion's target blocks actually drops.
 *
 * Vanilla drops are rolled on every call, as live mining rolls them on every
 * break; caching one roll would credit the same random result forever. A custom
 * block's declared `drops:` never change, so only those are cached.
 *
 * Both resolvers are injected so the cache can be tested without a server.
 *
 * Minions resume on every region at once under Folia, so the cache is concurrent.
 */
class MinionDropTables(
    /** Rolls what a vanilla block drops when broken, as live mining does. */
    private val resolve: (Material, Location) -> List<ItemStack>,
    /** Resolves one id from a definition's `drops:`, or null if this server has no such item. */
    private val resolveItem: (String) -> ItemStack?
) {
    private val cache: MutableMap<Key, List<ItemStack>> = ConcurrentHashMap()

    /**
     * @return What one [block] is worth to [definition] while nobody is online.
     *
     * A custom block's loot table cannot be read offline, which is what its
     * `drops:` list is for. A custom block with no `drops:` credits nothing:
     * there is no correct answer, and guessing an item is worse than none.
     */
    fun dropsFor(definition: MinionDefinition, block: MinionBlock, location: Location): List<ItemStack> =
        configuredDrops(definition, block)
            ?: block.vanillaMaterial?.let { resolve(it, location) }.orEmpty()

    /**
     * @return [block]'s declared `drops:`, or null if it declares none.
     *
     * Live mining uses these too, ahead of what the custom block plugin rolls
     * through eco, so an admin can always override a block's drops.
     */
    fun configuredDrops(definition: MinionDefinition, block: MinionBlock): List<ItemStack>? {
        if (block.drops.isEmpty()) {
            return null
        }
        return cache.computeIfAbsent(Key(definition.id, block.id)) { block.drops.mapNotNull(resolveItem) }
            .map { it.clone() }
    }

    fun clear() {
        cache.clear()
    }

    private data class Key(val definitionId: String, val blockId: String)
}
