package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.items.TestableItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * What a minion's drops are worth.
 *
 * Structured like [TargetSet], for a weaker version of the same reason: a sale
 * walks every occupied storage slot, so a linear scan of TestableItems per slot
 * is bounded by slots times prices rather than by anything per-tick. The
 * material index is kept anyway, because the vanilla case is nearly all of it.
 */
class PriceList(
    val entries: List<Entry>
) {
    /** One `sell-prices:` line: an item, its price, and its material if it has one. */
    data class Entry(
        val item: TestableItem,
        val price: Double,
        /** Null for a custom item, which cannot be indexed by material. */
        val vanillaMaterial: Material?
    )

    private val byMaterial: Map<Material, Double> =
        entries.mapNotNull { entry -> entry.vanillaMaterial?.let { it to entry.price } }.toMap()

    private val custom: List<Entry> = entries.filter { it.vanillaMaterial == null }

    val isEmpty: Boolean
        get() = entries.isEmpty()

    /** @return What one of [item] is worth, or null if this minion cannot sell it. */
    fun priceOf(item: ItemStack): Double? =
        byMaterial[item.type] ?: custom.firstOrNull { it.item.matches(item) }?.price

    companion object {
        val EMPTY = PriceList(emptyList())
    }
}
