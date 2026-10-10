package com.exanthiax.ecominions.minions.model

import org.bukkit.inventory.ItemStack

/** One real kill, kept so offline cycles can replay what this slayer actually earns. */
data class KillRecord(val drops: List<ItemStack>, val experience: Int)

object KillSample {
    const val SIZE = 10

    fun append(sample: List<KillRecord>, record: KillRecord): List<KillRecord> = (sample + record).takeLast(SIZE)
}
