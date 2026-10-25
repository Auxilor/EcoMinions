package com.exanthiax.ecominions.core.world

import com.exanthiax.ecominions.minions.model.VanillaFishingTable
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext
import org.bukkit.loot.LootTables
import java.util.concurrent.ThreadLocalRandom

/** The live server's own drops, loot and xp orbs, for the parts of a minion's work that copy vanilla. */
object VanillaWorld {
    private const val XP_MERGE_RADIUS = 1.5

    /**
     * What a vanilla block drops when broken, rolled the way live mining rolls it.
     *
     * A `blocks/` loot table cannot be populated through Bukkit's loot context, so
     * the drops come from a snapshot of the block at [location] retyped to
     * [material]; the world itself is never changed.
     */
    fun drops(material: Material, location: Location): List<ItemStack> =
        runCatching {
            location.block.state
                .apply { type = material }
                .drops
                .toList()
        }.getOrDefault(emptyList())

    /** Adds to an orb already at the minion, so an unattended minion keeps one orb rather than a pile. */
    fun spawnXpOrb(minion: Minion, amount: Int) {
        val location = minion.entity.location.clone().add(0.0, 0.5, 0.0)
        val existing = location.getNearbyEntitiesByType(ExperienceOrb::class.java, XP_MERGE_RADIUS)
            .firstOrNull { it.isValid }
        if (existing != null) {
            existing.experience += amount
        } else {
            minion.entity.world.spawn(location, ExperienceOrb::class.java).experience = amount
        }
    }

    /**
     * Rolls the server's own fishing loot table. Null when it cannot be rolled
     * without a hook, and the caller falls back to [VanillaFishingTable].
     */
    fun fishingLoot(location: Location, owner: Player?): List<ItemStack>? =
        runCatching {
            val context = LootContext.Builder(location).luck(0f).apply { if (owner != null) killer(owner) }.build()
            LootTables.FISHING.lootTable.populateLoot(ThreadLocalRandom.current(), context).toList()
        }.getOrNull()
}
