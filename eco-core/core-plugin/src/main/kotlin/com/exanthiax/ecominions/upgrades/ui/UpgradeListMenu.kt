package com.exanthiax.ecominions.upgrades.ui

import com.exanthiax.ecominions.api.event.MinionCloseEvent
import com.exanthiax.ecominions.api.event.MinionOpenEvent
import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.ui.MenuChrome
import com.exanthiax.ecominions.minions.ui.SlotListArea
import com.exanthiax.ecominions.minions.ui.addPagedAreaIfNotEmpty
import com.exanthiax.ecominions.upgrades.integration.UpgradePlaceholders
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeMenuSettings
import com.exanthiax.ecominions.upgrades.service.UpgradeService
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.component.addComponent
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.onClose
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
    private val upgradeMenu: UpgradeMenu,
    private val events: EventDispatcher
) {
    /** Set by the composition root: menus navigate back to the minion's main menu. */
    var onBack: ((Player, Minion) -> Unit)? = null

    fun open(player: Player, minion: Minion) {
        if (!events.dispatch(MinionOpenEvent(player, minion, MinionMenuType.UPGRADE_LIST))) {
            return
        }

        val current = settings.value
        val rows = current.listRows
        val context = TemplateContext.of(MinionPlaceholders.MINION to minion)
        val upgradeArea = chrome.area(current.upgradeListSection, "upgrade-area", rows)
        val allUpgrades = upgrades.allowedFor(minion) + minion.state.definition.compactorRecipes.map { it.upgrade }

        menu(rows) {
            setTitle(messages.render(current.listTitle, context))
            setMask(chrome.mask(current.upgradeListSection, rows))
            addCustomSlots(current.upgradeListSection, "")
            onClose { event, _ ->
                events.dispatch(MinionCloseEvent(event.player as Player, minion, MinionMenuType.UPGRADE_LIST))
            }

            addPagedAreaIfNotEmpty(
                SlotListArea(upgradeArea, allUpgrades.take(upgradeArea.size).map { upgradeSlot(player, minion, it) })
            )

            addComponent(chrome.backButton(current.upgradeListSection, rows) { onBack?.invoke(player, minion) })
        }.open(player)
    }

    private fun upgradeSlot(player: Player, minion: Minion, upgrade: Upgrade): Slot {
        val tier = upgrades.tierOf(minion, upgrade)
        val nextCosts = upgrades.nextTier(minion, upgrade)?.costs
        val context = TemplateContext.of(
            UpgradePlaceholders.UPGRADE to upgrade,
            "tier" to tier,
            "multi_pay_indicator" to if ((nextCosts?.size ?: 0) > 1) messages.line("gui.main.multi-pay-indicator") else "",
            "next_cost" to (
                nextCosts?.joinToString(messages.line("gui.main.upgrade-button.cost-separator")) { it.display(player) }
                    ?: messages.line("gui.main.upgrade-button.maxed")
                )
        )

        val item = Items.lookup(upgrade.iconLookup).item.clone()
        item.itemMeta?.let { meta ->
            meta.setDisplayName(messages.line("gui.main.upgrade-button.name", context))
            meta.lore = messages.linesWithout("gui.main.upgrade-button.lore", "multi_pay_indicator", context)
            item.itemMeta = meta
        }

        return slot(item) {
            onLeftClick { _, _ -> upgradeMenu.open(player, minion, upgrade) }
        }
    }
}
