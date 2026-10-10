package com.exanthiax.ecominions.minions.model

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Vanilla's fishing odds, for when the server's own loot table cannot be rolled
 * without a hook. Enchanted treasure is left out: it needs the enchanting roll.
 */
object VanillaFishingTable {
    private class Entry(val material: Material, val weight: Int)

    private val FISH = listOf(
        Entry(Material.COD, 60),
        Entry(Material.SALMON, 25),
        Entry(Material.TROPICAL_FISH, 2),
        Entry(Material.PUFFERFISH, 13)
    )

    private val JUNK = listOf(
        Entry(Material.LILY_PAD, 17),
        Entry(Material.LEATHER_BOOTS, 10),
        Entry(Material.LEATHER, 10),
        Entry(Material.BONE, 10),
        Entry(Material.STRING, 5),
        Entry(Material.FISHING_ROD, 2),
        Entry(Material.BOWL, 10),
        Entry(Material.STICK, 5),
        Entry(Material.INK_SAC, 1),
        Entry(Material.TRIPWIRE_HOOK, 10),
        Entry(Material.ROTTEN_FLESH, 10)
    )

    private val TREASURE = listOf(
        Entry(Material.NAME_TAG, 1),
        Entry(Material.NAUTILUS_SHELL, 1),
        Entry(Material.SADDLE, 1)
    )

    internal val fish: Set<Material> = FISH.map { it.material }.toSet()
    internal val junk: Set<Material> = JUNK.map { it.material }.toSet()
    internal val treasure: Set<Material> = TREASURE.map { it.material }.toSet()

    fun roll(random: Random): ItemStack {
        val category = random.nextInt(100)
        val pool = when {
            category < FISH_PERCENT -> FISH
            category < FISH_PERCENT + JUNK_PERCENT -> JUNK
            else -> TREASURE
        }
        var remaining = random.nextInt(pool.sumOf { it.weight })
        for (entry in pool) {
            remaining -= entry.weight
            if (remaining < 0) {
                return ItemStack(entry.material)
            }
        }
        return ItemStack(pool.last().material)
    }

    private const val FISH_PERCENT = 85
    private const val JUNK_PERCENT = 10
}
