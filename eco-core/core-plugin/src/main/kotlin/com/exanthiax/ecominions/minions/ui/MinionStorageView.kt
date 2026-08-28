package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Aggregates a minion's storage for display: one entry per distinct item,
 * biggest first.
 *
 * Grouped by whether two stacks would stack, not by material: a compacted or
 * custom item carries meta that a bare material throws away, and the display
 * has to show what the player will actually withdraw.
 *
 * Separate from [MinionStorageView] so it can be tested without eco's menu
 * types, and driven by what is stored rather than by what is priced - a minion
 * keeps everything it mines, so the display has to as well.
 */
object StorageContents {
    /**
     * @param similar Whether two stacks count as the same entry. Injected
     *        because a unit test's fake [ItemStack] cannot answer `isSimilar`.
     * @return One sample stack per distinct item, each with the total stored,
     *         highest total first. The samples are copies: mutating them cannot
     *         reach the minion's storage.
     */
    fun top(
        storage: MinionStorage,
        limit: Int,
        similar: (ItemStack, ItemStack) -> Boolean = { a, b -> a.isSimilar(b) }
    ): List<Pair<ItemStack, Int>> {
        val samples = mutableListOf<ItemStack>()
        val counts = mutableListOf<Int>()

        for (index in 0 until storage.size) {
            val item = storage.getItem(index) ?: continue
            if (item.type == Material.AIR || item.amount <= 0) {
                continue
            }

            val existing = samples.indexOfFirst { similar(it, item) }
            if (existing >= 0) {
                counts[existing] += item.amount
            } else {
                samples += item.clone().apply { amount = 1 }
                counts += item.amount
            }
        }

        return samples.indices
            .sortedByDescending { counts[it] }
            .take(limit.coerceAtLeast(0))
            .map { samples[it] to counts[it] }
    }
}

/**
 * Renders minion storage as one display slot per stored item, showing how much
 * has been collected, rather than exposing the raw storage grid.
 */
class MinionStorageView(
    private val messages: Messages
) {
    /**
     * @return One display item per distinct stored item, highest count first.
     *
     * Called on every menu render rather than once at open, so the counts track
     * what the minion is mining while the player watches.
     */
    fun itemsFor(minion: Minion, settings: MenuSettings, capacity: Int): List<ItemStack> =
        StorageContents.top(minion.storage, minOf(settings.storageSlots, capacity))
            .map { (sample, count) -> buildItem(sample, count, settings.storageDisplay) }

    /**
     * Builds the display item from the stored stack itself, so a compacted or
     * custom item keeps its name, its lore and the rest of its meta instead of
     * being redrawn as a plain material.
     */
    private fun buildItem(sample: ItemStack, count: Int, display: MenuSettings.StorageDisplay): ItemStack {
        val item = sample.clone()
        val meta = item.itemMeta ?: return item
        val context = TemplateContext.of(
            "item_name" to nameOf(item, if (meta.hasDisplayName()) meta.displayName else null),
            "count" to count
        )

        if (display.showsCountInName) {
            meta.setDisplayName(messages.line("gui.main.storage-item.name", context))
        }
        if (display.showsCountInLore) {
            // Appended rather than assigned: the item's own lore is part of what
            // it is, and a compacted item's tier or source often lives there.
            meta.lore = (meta.lore ?: emptyList()) + messages.lines("gui.main.storage-item.lore", context)
        }

        item.itemMeta = meta
        return item
    }

    /** The item's own name if it has one, otherwise its material read out. */
    private fun nameOf(item: ItemStack, displayName: String?): String =
        if (displayName.isNullOrEmpty()) titleCase(item.type) else displayName

    /** `SMOOTH_STONE` reads as `Smooth Stone`. */
    private fun titleCase(material: Material): String =
        material.name
            .lowercase()
            .split("_")
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
}
