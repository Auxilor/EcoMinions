package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.text.Messages
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** The shared furniture every menu in the plugin uses. */
class MenuChrome(
    private val messages: Messages
) {
    fun fillerMask(rows: Int): FillerMask =
        FillerMask(MaskItems(fillerMaterial()), *Array(rows) { FULL_ROW })

    /**
     * The same item [fillerMask] paints, for slots that are set on every render
     * and so have to draw their own background when they have nothing to show.
     */
    fun fillerItem(): ItemStack = ItemStack(fillerMaterial())

    fun backButton(): ItemStack = named(Material.ARROW, messages.line("gui.upgrade.back.name"))

    fun named(material: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(name)
        if (lore.isNotEmpty()) {
            meta.lore = lore
        }
        item.itemMeta = meta
        return item
    }

    private fun fillerMaterial(): Material =
        Material.matchMaterial(messages.raw("gui.filler.material").uppercase())
            ?: Material.BLACK_STAINED_GLASS_PANE

    private companion object {
        const val FULL_ROW = "111111111"
    }
}
