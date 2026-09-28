package com.exanthiax.ecominions.upgrades.ui

import com.exanthiax.ecominions.api.event.MinionCloseEvent
import com.exanthiax.ecominions.api.event.MinionOpenEvent
import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.minions.ui.MenuChrome
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeMenuSettings
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.onClose
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.Material
import org.bukkit.entity.Player

/** Shown when a tier can be paid for in more than one way. */
class CostSelectMenu(
    private val settings: SettingsHolder<UpgradeMenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val purchases: MinionUpgradeService,
    private val events: EventDispatcher
) {
    /** Set by the composition root: where to return after a successful purchase. */
    var onPurchased: ((Player, Minion, Upgrade) -> Unit)? = null

    fun open(player: Player, minion: Minion, upgrade: Upgrade, tier: UpgradeTier) {
        if (!events.dispatch(MinionOpenEvent(player, minion, MinionMenuType.COST_SELECT))) {
            return
        }

        val current = settings.value
        val rows = current.costSelectRows

        menu(rows) {
            setTitle(messages.render(current.costSelectTitle))
            setMask(chrome.fillerMask(rows))
            onClose { event, _ ->
                events.dispatch(MinionCloseEvent(event.player as Player, minion, MinionMenuType.COST_SELECT))
            }

            for ((index, cost) in tier.costs.withIndex()) {
                val column = FIRST_COLUMN + index
                if (column > LAST_COLUMN) {
                    break
                }
                setSlot(OPTION_ROW, column, optionSlot(player, minion, upgrade, tier, cost))
            }
        }.open(player)
    }

    private fun optionSlot(
        player: Player,
        minion: Minion,
        upgrade: Upgrade,
        tier: UpgradeTier,
        cost: TierCost
    ): Slot {
        val affordable = cost.canAfford(player)
        val status = if (affordable) {
            messages.line("gui.cost-select.option.affordable")
        } else {
            messages.line("gui.cost-select.option.not-affordable")
        }
        val context = TemplateContext.of(
            "display" to cost.display(player),
            "status" to status
        )

        val item = chrome.named(
            Material.PAPER,
            messages.line("gui.cost-select.option.name", context),
            messages.lines("gui.cost-select.option.lore", context)
        )

        return slot(item) {
            onLeftClick { _, _ ->
                when (purchases.purchase(player, minion, upgrade, tier, cost)) {
                    MinionUpgradeService.Result.PURCHASED -> onPurchased?.invoke(player, minion, upgrade)
                    MinionUpgradeService.Result.CANCELLED -> Unit
                    else -> player.sendMessage(messages.line("messages.upgrade-cant-afford"))
                }
            }
        }
    }

    private companion object {
        const val OPTION_ROW = 2
        const val FIRST_COLUMN = 2
        const val LAST_COLUMN = 8
    }
}
