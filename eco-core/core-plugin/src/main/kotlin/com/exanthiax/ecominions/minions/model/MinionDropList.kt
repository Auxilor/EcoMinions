package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.minions.service.activity.OfflineYield
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * A minion's own `drops:` list, in the same shape as EcoItems `loots/` items,
 * so admins learn one format. Each entry rolls its own chance.
 */
class MinionDropList(val entries: List<Entry>) {
    data class Entry(
        val item: ItemStack,
        val chance: Double,
        val amount: IntRange,
        val experience: IntRange?
    )

    val isEmpty: Boolean
        get() = entries.isEmpty()

    fun roll(random: Random = Random.Default): OfflineYield {
        val hits = entries.filter { random.nextDouble() < it.chance }
        return OfflineYield(
            drops = hits.map { entry ->
                entry.item.clone().apply { amount = entry.amount.random(random).coerceIn(1, type.maxStackSize) }
            },
            experience = hits.sumOf { it.experience?.random(random) ?: 0 }
        )
    }

    companion object {
        val EMPTY = MinionDropList(emptyList())
    }
}
