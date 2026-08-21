package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Aggregates a minion's storage for display: one entry per material, biggest
 * first.
 *
 * Separate from [MinionStorageView] so it can be tested without eco's menu
 * types, and driven by what is stored rather than by what is priced - a minion
 * keeps everything it mines, so the display has to as well.
 */
object StorageContents {
    fun top(storage: MinionStorage, limit: Int): List<Pair<Material, Int>> {
        val counts = LinkedHashMap<Material, Int>()

        for (index in 0 until storage.size) {
            val item = storage.getItem(index) ?: continue
            if (item.type == Material.AIR || item.amount <= 0) {
                continue
            }
            counts[item.type] = (counts[item.type] ?: 0) + item.amount
        }

        return counts.entries
            .sortedByDescending { it.value }
            .take(limit.coerceAtLeast(0))
            .map { it.key to it.value }
    }
}

/**
 * Renders minion storage as one display slot per stored item type, showing how
 * much has been collected, rather than exposing the raw storage grid.
 */
class MinionStorageView(
    private val messages: Messages
) {
    /** @return One non-interactive slot per stored material, highest count first. */
    fun slotsFor(minion: Minion, settings: MenuSettings, capacity: Int): List<Slot> =
        StorageContents.top(minion.storage, minOf(settings.storageSlots, capacity))
            .map { (material, count) -> buildSlot(material, count, settings.storageDisplay) }

    private fun buildSlot(material: Material, count: Int, display: MenuSettings.StorageDisplay): Slot {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return slot(item)
        val context = TemplateContext.of(
            "item_name" to titleCase(material),
            "count" to count
        )

        meta.setDisplayName(
            if (display.showsCountInName) {
                messages.line("gui.main.storage-item.name", context)
            } else {
                titleCase(material)
            }
        )
        if (display.showsCountInLore) {
            meta.lore = messages.lines("gui.main.storage-item.lore", context)
        }

        item.itemMeta = meta
        return slot(item)
    }

    /** `SMOOTH_STONE` reads as `Smooth Stone`. */
    private fun titleCase(material: Material): String =
        material.name
            .lowercase()
            .split("_")
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
}
