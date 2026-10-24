package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.text.Messages
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.SlotArea
import com.willfp.eco.core.gui.SlotLocation
import com.willfp.eco.core.gui.component.ConfigButton
import com.willfp.eco.core.gui.component.PagedAreaComponent
import com.willfp.eco.core.gui.component.addPagedArea
import com.willfp.eco.core.gui.getFillerMask
import com.willfp.eco.core.gui.getSlotArea
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** The shared furniture every menu in the plugin uses. */
class MenuChrome(
    private val messages: Messages
) {
    fun fillerMask(rows: Int): FillerMask =
        FillerMask(MaskItems(fillerMaterial()), *Array(rows) { FULL_ROW })

    /** The mask set by the `mask` keys of [section], or [fillerMask] when it sets none. */
    fun mask(section: Config, rows: Int): FillerMask =
        if (section.has("mask")) section.getFillerMask() else fillerMask(rows)

    /**
     * The same item [fillerMask] paints, for slots that are set on every render
     * and so have to draw their own background when they have nothing to show.
     */
    fun fillerItem(): ItemStack = ItemStack(fillerMaterial())

    /** The area set by the [path] section of [section], or everything between the title row and the button row. */
    fun area(section: Config, path: String, rows: Int): SlotArea =
        if (section.has(path)) {
            section.getSlotArea(path)
        } else {
            SlotArea(
                row = AREA_ROW,
                column = AREA_COLUMN,
                rows = (rows - AREA_ROW).coerceAtLeast(0),
                columns = AREA_COLUMNS
            )
        }

    /** The back button set by the `back` section of [section], in the bottom left corner by default. */
    fun backButton(section: Config, rows: Int, action: () -> Unit): ConfigButton =
        ConfigButton(
            section.getSubsection("back"),
            defaultItem = "arrow",
            name = messages.line("gui.upgrade.back.name"),
            defaultLocations = listOf(SlotLocation(row = rows, column = 1))
        ) { _, _, _, _ -> action() }

    /** The item named by the `item` or `material` key of [section], or [default] when it names none. */
    fun buttonItem(section: Config, default: Material): ItemStack =
        (section.getStringOrNull("item") ?: section.getStringOrNull("material"))
            ?.let { Items.lookup(it).item }
            ?: ItemStack(default)

    fun named(material: Material, name: String, lore: List<String> = emptyList()): ItemStack =
        named(ItemStack(material), name, lore)

    fun named(item: ItemStack, name: String, lore: List<String> = emptyList()): ItemStack {
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
        const val AREA_ROW = 2
        const val AREA_COLUMN = 2
        const val AREA_COLUMNS = 7
    }
}

/** Set [slot] at every one of [locations]. */
fun MenuBuilder.setSlots(locations: List<SlotLocation>, slot: Slot): MenuBuilder {
    for ((row, column) in locations) {
        setSlot(row, column, slot)
    }

    return this
}

/** Add [component] unless its area is empty, so a menu too short for its area still opens. */
fun MenuBuilder.addPagedAreaIfNotEmpty(component: PagedAreaComponent<*>): MenuBuilder =
    if (component.area.size > 0) addPagedArea(component) else this

/** An area showing [slots] in order, row by row. */
class SlotListArea(
    area: SlotArea,
    private val slots: List<Slot>
) : PagedAreaComponent<Slot>(area) {
    override fun getEntries(player: Player, menu: Menu): List<Slot> = slots

    override fun getEntrySlot(player: Player, menu: Menu, entry: Slot): Slot = entry
}
