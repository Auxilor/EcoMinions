package com.exanthiax.ecominions.minions.model

import org.bukkit.inventory.ItemStack
import wiki.ecohub.game.entity.lookup.TestableEntity
import wiki.ecohub.game.item.lookup.TestableItem

/** What a breeder breeds and feeds on. [maxMobs] may be a `%level%` expression. */
data class BreederSettings(
    val targets: List<TestableEntity>,
    val food: List<TestableItem>,
    val maxMobs: String
)

fun BreederSettings.accepts(item: ItemStack): Boolean = food.any { it.matches(item) }
