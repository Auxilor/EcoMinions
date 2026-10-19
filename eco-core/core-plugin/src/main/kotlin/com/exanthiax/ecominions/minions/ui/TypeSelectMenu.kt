package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionItemService
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.Material
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
        val pages = minions.types()
            .chunked(((rows - FIRST_ROW) * COLUMNS).coerceAtLeast(COLUMNS))
            .ifEmpty { listOf(emptyList()) }

        menu(rows) {
            setTitle(messages.render(current.typeSelectTitle))
            setMask(chrome.fillerMask(rows))
            maxPages(pages.size)

            addPageChanger(
                PageChanger.Direction.BACKWARDS,
                chrome.named(Material.ARROW, messages.line("gui.type-select.previous-page.name")),
                null,
                null,
                rows,
                1
            )
            addPageChanger(
                PageChanger.Direction.FORWARDS,
                chrome.named(Material.ARROW, messages.line("gui.type-select.next-page.name")),
                null,
                null,
                rows,
                9
            )

            pages.forEachIndexed { index, types ->
                addPage(index + 1) {
                    if (current.typeSelectSection.has("mask")) {
                        setMask(current.typeSelectSection)
                    }
                    addCustomSlots(current.typeSelectSection, "")
                    for ((position, type) in types.withIndex()) {
                        setSlot(
                            FIRST_ROW + (position / COLUMNS),
                            FIRST_COLUMN + (position % COLUMNS),
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
        const val FIRST_ROW = 2
        const val FIRST_COLUMN = 2
        const val COLUMNS = 7
        const val GIVE_PERMISSION = "ecominions.command.give"
    }
}
