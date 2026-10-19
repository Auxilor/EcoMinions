package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionItemService
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.gui.SlotLocation
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.entity.Player

/** Browse and take one of every configured minion. */
class TypeSelectMenu(
    private val settings: SettingsHolder<MenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val minions: MinionService,
    private val itemFactory: MinionItemFactory,
    private val items: MinionItemService
) {
    fun open(player: Player) {
        val current = settings.value
        val rows = current.typeSelectRows
        val typeArea = chrome.area(current.typeSelectSection, "type-area", rows)
        val columns = typeArea.columns.coerceAtLeast(1)
        val pages = minions.types()
            .chunked(typeArea.size.coerceAtLeast(columns))
            .ifEmpty { listOf(emptyList()) }

        menu(rows) {
            setTitle(messages.render(current.typeSelectTitle))
            setMask(chrome.fillerMask(rows))
            maxPages(pages.size)

            addPageChanger(
                current.typeSelectSection,
                "previous-page",
                PageChanger.Direction.BACKWARDS,
                null,
                defaultItem = "arrow",
                name = messages.line("gui.type-select.previous-page.name"),
                defaultLocations = listOf(SlotLocation(rows, 1)),
                hideInactive = true
            )
            addPageChanger(
                current.typeSelectSection,
                "next-page",
                PageChanger.Direction.FORWARDS,
                null,
                defaultItem = "arrow",
                name = messages.line("gui.type-select.next-page.name"),
                defaultLocations = listOf(SlotLocation(rows, 9)),
                hideInactive = true
            )

            pages.forEachIndexed { index, types ->
                addPage(index + 1) {
                    if (current.typeSelectSection.has("mask")) {
                        setMask(current.typeSelectSection)
                    }
                    addCustomSlots(current.typeSelectSection, "")
                    for ((position, type) in types.withIndex()) {
                        setSlot(
                            typeArea.row + (position / columns),
                            typeArea.column + (position % columns),
                            typeSlot(player, type)
                        )
                    }
                }
            }
        }.open(player)
    }

    private fun typeSlot(player: Player, type: MinionDefinition): Slot {
        val state = MinionState(definition = type)
        val item = itemFactory.build(state)
        val context = TemplateContext.of(
            "type_name" to type.name,
            "targets" to type.targetBlockIds.joinToString(),
            "speed" to type.baseSpeedTicks,
            "capacity" to type.baseStorageSlots
        )

        item.itemMeta?.let { meta ->
            meta.setDisplayName(messages.line("gui.type-select.title-item.name", context))
            meta.lore = messages.lines("gui.type-select.title-item.lore", context)
            item.itemMeta = meta
        }

        return slot(item) {
            onLeftClick { event, _ ->
                event.whoClicked.closeInventory()
                if (!player.hasPermission(GIVE_PERMISSION)) {
                    player.sendMessage(messages.line("messages.no-permission"))
                    return@onLeftClick
                }
                items.give(player, type, 1, 1)
            }
        }
    }

    private companion object {
        const val GIVE_PERMISSION = "ecominions.command.give"
    }
}
