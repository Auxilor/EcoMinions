package com.exanthiax.ecominions.upgrades.ui

import com.exanthiax.ecominions.api.event.MinionCloseEvent
import com.exanthiax.ecominions.api.event.MinionOpenEvent
import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.minions.ui.MenuChrome
import com.exanthiax.ecominions.minions.ui.SlotListArea
import com.exanthiax.ecominions.minions.ui.addPagedAreaIfNotEmpty
import com.exanthiax.ecominions.minions.ui.setSlots
import com.exanthiax.ecominions.upgrades.integration.UpgradePlaceholders
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeMenuSettings
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.exanthiax.ecominions.upgrades.service.UpgradeService
import com.willfp.eco.core.gui.SlotLocation
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.component.addComponent
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.onClose
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.util.toNumeral
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** One upgrade's tier ladder, and the buy action for the next rung. */
class UpgradeMenu(
    private val settings: SettingsHolder<UpgradeMenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val upgrades: UpgradeService,
    private val purchases: MinionUpgradeService,
    private val costSelect: CostSelectMenu,
    private val events: EventDispatcher,
    private val regions: Regions
) {
    /** Set by the composition root: where the back button leads. */
    var onBack: ((Player, Minion) -> Unit)? = null

    fun open(player: Player, minion: Minion, upgrade: Upgrade) {
        if (!events.dispatch(MinionOpenEvent(player, minion, MinionMenuType.UPGRADE))) {
            return
        }

        val current = settings.value
        val rows = current.upgradeRows
        val owned = upgrades.tierOf(minion, upgrade)
        val tierArea = chrome.area(current.upgradeSection, "tier-area", rows)
        val context = TemplateContext.of(
            UpgradePlaceholders.UPGRADE to upgrade,
            "tier" to owned
        )

        menu(rows) {
            setTitle(messages.render(current.upgradeTitle, context))
            setMask(chrome.mask(current.upgradeSection, rows))
            addCustomSlots(current.upgradeSection, "")
            onClose { event, _ ->
                events.dispatch(MinionCloseEvent(event.player as Player, minion, MinionMenuType.UPGRADE))
            }
            setSlots(
                current.upgradeSection.getSlotLocations(
                    "title-item",
                    listOf(SlotLocation(row = 1, column = TITLE_COLUMN))
                ),
                slot(titleItem(upgrade, context))
            )

            addPagedAreaIfNotEmpty(
                SlotListArea(
                    tierArea,
                    upgrade.orderedTiers().take(tierArea.size).map { tierSlot(player, minion, upgrade, it, owned) }
                )
            )

            addComponent(chrome.backButton(current.upgradeSection, rows) { onBack?.invoke(player, minion) })
        }.open(player)
    }

    private fun titleItem(upgrade: Upgrade, context: TemplateContext): ItemStack {
        val item = Items.lookup(upgrade.iconLookup).item.clone()
        item.itemMeta?.let { meta ->
            meta.setDisplayName(messages.line("gui.upgrade.title-item.name", context))
            meta.lore = messages.lines("gui.upgrade.title-item.lore", context)
            item.itemMeta = meta
        }
        return item
    }

    private fun tierSlot(
        player: Player,
        minion: Minion,
        upgrade: Upgrade,
        tier: UpgradeTier,
        owned: Int
    ): Slot {
        val state = TierState.of(tier.level, owned)
        val item = chrome.named(
            state.material,
            messages.render("${prefixFor(state)}${upgrade.name} ${tier.level.toNumeral()}"),
            tierLore(player, upgrade, tier)
        )

        return slot(item) {
            if (state == TierState.NEXT) {
                onLeftClick { _, _ -> buy(player, minion, upgrade, tier) }
            } else if (owned >= upgrade.maxTier) {
                onLeftClick { _, _ -> player.sendMessage(messages.line("messages.upgrade-already-max")) }
            }
        }
    }

    private fun tierLore(player: Player, upgrade: Upgrade, tier: UpgradeTier): List<String> {
        val lore = mutableListOf<String>()
        val context = TemplateContext.of(
            UpgradePlaceholders.UPGRADE to upgrade,
            UpgradePlaceholders.TIER to tier
        )

        if (tier.statDelta.isNotBlank()) {
            lore += messages.line("gui.upgrade.tier.stat-delta-line", context)
        }
        for (cost in tier.costs) {
            lore += messages.line(
                "gui.upgrade.tier.cost-line",
                context.plus("cost" to cost.display(player))
            )
        }
        return lore
    }

    private fun buy(player: Player, minion: Minion, upgrade: Upgrade, tier: UpgradeTier) {
        when (tier.costs.size) {
            0 -> player.sendMessage(messages.line("messages.no-payment-method"))
            1 -> {
                if (!regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
                    return
                }
                val result = purchases.purchase(player, minion, upgrade, tier, tier.costs.first())
                finishPurchase(player, minion, upgrade, tier, result)
            }

            else -> costSelect.open(player, minion, upgrade, tier)
        }
    }

    /** Tells [player] how buying [tier] went, and reopens the upgrade after a purchase. */
    fun finishPurchase(
        player: Player,
        minion: Minion,
        upgrade: Upgrade,
        tier: UpgradeTier,
        result: MinionUpgradeService.Result
    ) {
        announce(player, result, upgrade, tier)
        if (result == MinionUpgradeService.Result.PURCHASED) {
            open(player, minion, upgrade)
        }
    }

    private fun announce(
        player: Player,
        result: MinionUpgradeService.Result,
        upgrade: Upgrade,
        tier: UpgradeTier
    ) {
        val context = TemplateContext.of(
            UpgradePlaceholders.UPGRADE to upgrade,
            "upgrade" to upgrade.name,
            "tier" to tier.level
        )
        when (result) {
            MinionUpgradeService.Result.PURCHASED ->
                player.sendMessage(messages.line("messages.upgrade-purchased", context))

            MinionUpgradeService.Result.CANNOT_AFFORD ->
                player.sendMessage(messages.line("messages.upgrade-cant-afford", context))

            MinionUpgradeService.Result.NOT_ALLOWED ->
                player.sendMessage(messages.line("messages.no-payment-method", context))

            MinionUpgradeService.Result.CONDITIONS_NOT_MET ->
                player.sendMessage(messages.line("messages.upgrade-conditions-not-met", context))

            MinionUpgradeService.Result.CANCELLED -> Unit
        }
    }

    private fun prefixFor(state: TierState): String = when (state) {
        TierState.CURRENT -> messages.raw("gui.upgrade.tier.current-prefix")
        TierState.NEXT -> messages.raw("gui.upgrade.tier.next-prefix")
        else -> messages.raw("gui.upgrade.tier.locked-prefix")
    }

    private enum class TierState(val material: Material) {
        CURRENT(Material.GOLD_BLOCK),
        OWNED(Material.LIME_STAINED_GLASS_PANE),
        NEXT(Material.YELLOW_STAINED_GLASS_PANE),
        LOCKED(Material.GRAY_STAINED_GLASS_PANE);

        companion object {
            fun of(level: Int, owned: Int): TierState = when {
                level == owned -> CURRENT
                level < owned -> OWNED
                level == owned + 1 -> NEXT
                else -> LOCKED
            }
        }
    }

    private companion object {
        const val TITLE_COLUMN = 5
        const val MENU_ACTION = "Buying a minion upgrade"
    }
}
