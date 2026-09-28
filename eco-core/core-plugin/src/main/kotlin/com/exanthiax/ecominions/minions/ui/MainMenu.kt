package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.api.event.MinionCloseEvent
import com.exanthiax.ecominions.api.event.MinionOpenEvent
import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionCollectService
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.upgrades.ui.UpgradeListMenu
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.onClose
import com.willfp.eco.core.gui.onOpen
import com.willfp.eco.core.gui.onRender
import com.willfp.eco.core.gui.onShiftLeftClick
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * The menu shown when a player clicks a minion.
 *
 * Every slot is provider-backed rather than a fixed item: the minion keeps
 * mining and burning fuel while the menu is open, so the items are rebuilt on
 * each render and [MenuViewers] drives those renders.
 */
class MainMenu(
    private val settings: SettingsHolder<MenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val storageView: MinionStorageView,
    private val placement: PlacementService,
    private val fuels: FuelService,
    private val fuelService: MinionFuelService,
    private val collector: MinionCollectService,
    private val upgradeList: UpgradeListMenu,
    private val viewers: MenuViewers,
    private val events: EventDispatcher
) {
    fun open(player: Player, minion: Minion) {
        if (!events.dispatch(MinionOpenEvent(player, minion, MinionMenuType.MAIN))) {
            return
        }

        val current = settings.value
        val rows = current.mainRows
        val positions = storagePositions(rows)

        // Built once per render and then read by each storage slot, so the
        // aggregation runs once for the whole area instead of once per slot.
        var contents: List<ItemStack> = emptyList()

        menu(rows) {
            setTitle(messages.render(current.mainTitle, context(minion)))
            setMask(chrome.fillerMask(rows))

            onRender { _, _ ->
                contents = storageView.itemsFor(minion, settings.value, positions.size)
            }

            setSlot(1, TITLE_COLUMN, slot({ _, _ -> titleItem(minion) }))

            positions.forEachIndexed { index, (row, column) ->
                setSlot(row, column, slot({ _, _ -> contents.getOrNull(index) ?: chrome.fillerItem() }))
            }

            setSlot(rows, 2, collectAllSlot(player, minion))
            setSlot(rows, 4, fuelSlot(player, minion))
            setSlot(rows, 5, pickupSlot(player, minion))
            setSlot(rows, 7, upgradesSlot(player, minion))
            setSlot(rows, 8, infoSlot(minion))

            onOpen { viewer, menu -> viewers.watch(viewer, menu) }
            onClose { event, menu ->
                val viewer = event.player as Player
                viewers.forget(viewer, menu)
                events.dispatch(MinionCloseEvent(viewer, minion, MinionMenuType.MAIN))
            }
        }.open(player)
    }

    /** The storage area is everything between the title row and the button row. */
    private fun storagePositions(rows: Int): List<Pair<Int, Int>> =
        (2 until rows).flatMap { row -> (2..8).map { column -> row to column } }

    private fun context(minion: Minion): TemplateContext =
        TemplateContext.of(MinionPlaceholders.MINION to minion)

    private fun titleItem(minion: Minion): ItemStack {
        val context = context(minion)
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(messages.line("gui.main.title-item.name", context))
        meta.lore = messages.lines("gui.main.title-item.lore", context)
        item.itemMeta = meta
        return item
    }

    private fun collectAllSlot(player: Player, minion: Minion): Slot =
        slot({ _, _ -> collectAllItem(minion) }) {
            // Refreshed here as well as on the loop so the storage area empties
            // the moment it is clicked, not on the next tick of the loop.
            onLeftClick { _, _, menu ->
                val collected = collector.collect(player, minion) ?: return@onLeftClick
                player.sendMessage(
                    messages.line("messages.collected", TemplateContext.of("count" to collected))
                )
                menu.refresh(player)
            }
        }

    private fun collectAllItem(minion: Minion): ItemStack {
        val context = context(minion)
        return chrome.named(
            Material.HOPPER,
            messages.line("gui.main.collect-all.name", context),
            messages.lines("gui.main.collect-all.lore", context)
        )
    }

    /**
     * Fuel goes in one item at a time, a stack at a time, or all at once.
     *
     * Dropping fuel onto this slot reaches the server as a click with the stack
     * held on the cursor, so that is what the click handlers read first; an
     * empty cursor, or something that is not fuel, falls back to taking fuel
     * out of the player's inventory.
     */
    private fun fuelSlot(player: Player, minion: Minion): Slot =
        slot({ _, _ -> fuelItem(minion) }) {
            onLeftClick { event, _, menu -> deposit(player, minion, event, menu, whole = true) }
            onRightClick { event, _, menu -> deposit(player, minion, event, menu, whole = false) }
            onShiftLeftClick { _, _, menu ->
                announce(player, fuelService.fuelFrom(player, minion, limit = Int.MAX_VALUE))
                menu.refresh(player)
            }
        }

    private fun deposit(player: Player, minion: Minion, event: InventoryClickEvent, menu: Menu, whole: Boolean) {
        val applied = fromCursor(player, minion, event, whole)
            ?.takeUnless { it == MinionFuelService.Outcome.NoFuel }
            ?: fuelService.fuelFrom(player, minion)

        announce(player, applied)
        menu.refresh(player)
    }

    /** @return What the cursor stack fuelled, or null if the cursor is empty. */
    private fun fromCursor(
        player: Player,
        minion: Minion,
        event: InventoryClickEvent,
        whole: Boolean
    ): MinionFuelService.Outcome? {
        val cursor = event.cursor
        if (cursor == null || cursor.type == Material.AIR) {
            return null
        }

        val count = if (whole) cursor.amount else 1
        val outcome = fuelService.fuelFromItem(player, minion, cursor, count)
        if (outcome is MinionFuelService.Outcome.Applied) {
            // The click is cancelled before this runs, so the cursor is ours to
            // shrink: nothing else is going to take the items we just burnt.
            val left = cursor.amount - outcome.application.count
            player.setItemOnCursor(if (left <= 0) null else cursor.clone().apply { amount = left })
        }
        return outcome
    }

    private fun announce(player: Player, outcome: MinionFuelService.Outcome) {
        val applied = when (outcome) {
            is MinionFuelService.Outcome.Applied -> outcome.application
            MinionFuelService.Outcome.NoFuel -> {
                player.sendMessage(messages.line("messages.no-fuel"))
                return
            }
            MinionFuelService.Outcome.Cancelled -> return
        }
        player.sendMessage(
            messages.line(
                "messages.fuel-applied",
                TemplateContext.of(
                    "fuel_name" to applied.fuel.displayName,
                    "count" to applied.count
                )
            )
        )
    }

    private fun fuelItem(minion: Minion): ItemStack {
        val active = fuels.activeFuelOf(minion)
        val lore = if (active == null) {
            messages.lines("gui.main.fuel.idle-lore")
        } else {
            messages.lines(
                "gui.main.fuel.active-lore",
                TemplateContext.of(
                    "fuel_name" to active.displayName,
                    "seconds" to fuels.remainingMillis(minion) / MILLIS_PER_SECOND,
                    "multiplier" to MULTIPLIER_FORMAT.format(active.speedMultiplier)
                )
            )
        }

        return chrome.named(
            if (active != null) Material.BLAZE_ROD else Material.COAL,
            messages.line("gui.main.fuel.name"),
            lore
        )
    }

    private fun pickupSlot(player: Player, minion: Minion): Slot {
        val item = chrome.named(
            Material.BARRIER,
            messages.line("gui.main.pickup.name"),
            messages.lines("gui.main.pickup.lore")
        )

        return slot(item) {
            onLeftClick { event, _ ->
                val picked = when (val result = placement.pickUp(player, minion)) {
                    is PlacementService.PickUpResult.PickedUp -> result.item
                    PlacementService.PickUpResult.NotOwner -> {
                        player.sendMessage(messages.line("messages.not-owner"))
                        return@onLeftClick
                    }
                    PlacementService.PickUpResult.Cancelled -> return@onLeftClick
                }
                player.inventory.addItem(picked).values.forEach { leftover ->
                    player.world.dropItemNaturally(player.location, leftover)
                }
                player.sendMessage(messages.line("messages.picked-up"))
                event.whoClicked.closeInventory()
            }
        }
    }

    private fun upgradesSlot(player: Player, minion: Minion): Slot {
        val item = chrome.named(
            Material.ANVIL,
            messages.line("gui.main.upgrades-button.name"),
            messages.lines("gui.main.upgrades-button.lore")
        )
        return slot(item) {
            onLeftClick { _, _ -> upgradeList.open(player, minion) }
        }
    }

    private fun infoSlot(minion: Minion): Slot =
        slot({ _, _ ->
            chrome.named(
                Material.BOOK,
                messages.line("gui.main.info.name"),
                messages.lines("gui.main.info.lore", context(minion))
            )
        })

    private companion object {
        const val TITLE_COLUMN = 5
        const val MILLIS_PER_SECOND = 1000L
        const val MULTIPLIER_FORMAT = "%.2fx"
    }
}
