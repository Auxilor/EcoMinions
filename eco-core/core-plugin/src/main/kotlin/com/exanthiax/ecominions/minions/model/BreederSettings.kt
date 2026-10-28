package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.entities.TestableEntity
import com.willfp.eco.core.items.TestableItem
import org.bukkit.inventory.ItemStack

/** What a breeder breeds and feeds on. [maxMobs] may be a `%level%` expression. */
data class BreederSettings(
    val targets: List<TestableEntity>,
    val food: List<TestableItem>,
    val maxMobs: String
)

fun BreederSettings.accepts(item: ItemStack): Boolean = food.any { it.matches(item) }
