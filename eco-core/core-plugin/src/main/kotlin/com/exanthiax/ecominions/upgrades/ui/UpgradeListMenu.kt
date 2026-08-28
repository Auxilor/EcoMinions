package com.exanthiax.ecominions.upgrades.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.ui.MenuChrome
import com.exanthiax.ecominions.upgrades.integration.UpgradePlaceholders
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeMenuSettings
import com.exanthiax.ecominions.upgrades.service.UpgradeService
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import org.bukkit.entity.Player

/** Every upgrade a minion is allowed to buy. */
class UpgradeListMenu(
    private val settings: SettingsHolder<UpgradeMenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val upgrades: UpgradeService,
    private val upgradeMenu: UpgradeMenu
) {
    /** Set by the composition root: menus navigate back to the minion's main menu. */
    var onBack: ((Player, Minion) -> Unit)? = null

    fun open(player: Player, minion: Minion) {
        val current = settings.value
        val rows = current.listRows
        val context = TemplateContext.of(MinionPlaceholders.MINION to minion)
        val allUpgrades = upgrades.allowedFor(minion) + minion.state.definition.compactorRecipes.map { it.upgrade }

        menu(rows) {
            setTitle(messages.render(current.listTitle, context))
            setMask(chrome.fillerMask(rows))

            for ((index, upgrade) in allUpgrades.withIndex()) {
                val row = FIRST_ROW + (index / COLUMNS)
                if (row >= rows) {
                    break
                }
                setSlot(row, FIRST_COLUMN + (index % COLUMNS), upgradeSlot(player, minion, upgrade))
            }

            setSlot(rows, 1, slot(chrome.backButton()) {
                onLeftClick { _, _ -> onBack?.invoke(player, minion) }
            })
        }.open(player)
    }

    private fun upgradeSlot(player: Player, minion: Minion, upgrade: Upgrade): Slot {
        val tier = upgrades.tierOf(minion, upgrade)
        val hasChoice = (upgrades.nextTier(minion, upgrade)?.costs?.size ?: 0) > 1
        val context = TemplateContext.of(
            UpgradePlaceholders.UPGRADE to upgrade,
            "tier" to tier,
            "multi_pay_indicator" to if (hasChoice) messages.line("gui.main.multi-pay-indicator") else ""
        )

        val item = Items.lookup(upgrade.iconLookup).item.clone()
        item.itemMeta?.let { meta ->
            meta.setDisplayName(messages.line("gui.main.upgrade-button.name", context))
            meta.lore = messages.lines("gui.main.upgrade-button.lore", context)
            item.itemMeta = meta
        }

        return slot(item) {
            onLeftClick { _, _ -> upgradeMenu.open(player, minion, upgrade) }
        }
    }

    private companion object {
        const val FIRST_ROW = 2
        const val FIRST_COLUMN = 2
        const val COLUMNS = 7
    }
}
