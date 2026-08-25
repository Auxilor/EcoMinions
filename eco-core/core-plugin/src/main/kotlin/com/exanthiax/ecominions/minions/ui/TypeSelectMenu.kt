package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.entity.Player

/** Browse and take one of every configured minion. */
class TypeSelectMenu(
    private val settings: SettingsHolder<MenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val minions: MinionService,
    private val itemFactory: MinionItemFactory
) {
    fun open(player: Player) {
        val current = settings.value
        val rows = current.typeSelectRows

        menu(rows) {
            setTitle(messages.render(current.typeSelectTitle))
            setMask(chrome.fillerMask(rows))

            for ((index, type) in minions.types().withIndex()) {
                val row = FIRST_ROW + (index / COLUMNS)
                if (row >= rows) {
                    break
                }
                setSlot(row, FIRST_COLUMN + (index % COLUMNS), typeSlot(player, type))
            }
        }.open(player)
    }

    private fun typeSlot(player: Player, type: MinionDefinition): Slot {
        val state = MinionState(type = type)
        val item = itemFactory.build(state)
        val context = TemplateContext.of(
            "type_name" to type.name,
            "targets" to type.targetBlocks.joinToString { it.name },
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
                player.inventory.addItem(itemFactory.build(state))
                event.whoClicked.closeInventory()
            }
        }
    }

    private companion object {
        const val FIRST_ROW = 2
        const val FIRST_COLUMN = 2
        const val COLUMNS = 7
    }
}
