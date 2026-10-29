package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.api.event.MinionCloseEvent
import com.exanthiax.ecominions.api.event.MinionOpenEvent
import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.accepts
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionCollectService
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionInput
import com.exanthiax.ecominions.minions.service.MinionLimits
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.upgrades.ui.UpgradeListMenu
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.SlotLocation
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.component.PagedAreaComponent
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.onClose
import com.willfp.eco.core.gui.onOpen
import com.willfp.eco.core.gui.onRender
import com.willfp.eco.core.gui.onShiftLeftClick
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
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
 *
 * Clicks change both the player and the minion, so they are refused on Folia
 * when the menu was opened on a minion in another region.
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
    private val events: EventDispatcher,
    private val regions: Regions,
    private val limits: MinionLimits,
    private val input: MinionInput
) {
    fun open(player: Player, minion: Minion) {
        if (!events.dispatch(MinionOpenEvent(player, minion, MinionMenuType.MAIN))) {
            return
        }

        val current = settings.value
        val rows = current.mainRows
        val storageArea = chrome.area(current.mainSection, "storage-area", rows)
        val emptyItem = emptySlotItem(minion.state.definition)

        // Built once per render and then read by each storage slot, so the
        // aggregation runs once for the whole area instead of once per slot.
        var entries: List<Pair<ItemStack, Int>> = emptyList()

        menu(rows) {
            setTitle(messages.render(current.mainTitle, context(minion)))
            setMask(chrome.mask(current.mainSection, rows))
            addCustomSlots(current.mainSection, "")

            onRender { _, _ ->
                entries = storageView.entriesFor(minion, settings.value, storageArea.size)
            }

            setButton(current.mainSection, key = "title-item", row = 1, column = TITLE_COLUMN) { section ->
                slot({ _, _ -> titleItem(minion, section) })
            }

            addPagedAreaIfNotEmpty(object : PagedAreaComponent<Pair<ItemStack, Int>>(storageArea) {
                override fun getEntries(player: Player, menu: Menu): List<Pair<ItemStack, Int>> = entries

                override fun getEntrySlot(player: Player, menu: Menu, entry: Pair<ItemStack, Int>): Slot =
                    inputSlot(
                        player = player,
                        minion = minion,
                        shown = storageView.displayItem(entry.first, entry.second, settings.value),
                        sample = entry.first
                    )

                override fun getEmptySlot(player: Player, menu: Menu): Slot =
                    inputSlot(player = player, minion = minion, shown = emptyItem.clone(), sample = null)
            })

            setButton(current.mainSection, key = "collect-all", row = rows, column = 2) { section ->
                collectAllSlot(player, minion, section)
            }
            setButton(current.mainSection, key = "fuel", row = rows, column = 4) { section ->
                fuelSlot(player, minion, section)
            }
            setButton(current.mainSection, key = "pickup", row = rows, column = 5) { section ->
                pickupSlot(player, minion, section)
            }
            setButton(current.mainSection, key = "upgrades", row = rows, column = 7) { section ->
                upgradesSlot(player, minion, section)
            }
            setButton(current.mainSection, key = "info", row = rows, column = 8) { section ->
                infoSlot(minion, section)
            }
            if (limits.inUse()) {
                setButton(current.mainSection, key = "limit", row = rows, column = 3) { limitSlot(minion) }
            }
            if (current.showStoredXp && messages.has("gui.main.stored-xp.name")) {
                setButton(current.mainSection, key = "stored-xp", row = rows, column = 6) { section ->
                    storedExperienceSlot(player, minion, section)
                }
            }

            onOpen { viewer, menu -> viewers.watch(viewer, menu) }
            onClose { event, menu ->
                val viewer = event.player as Player
                viewers.forget(viewer, menu)
                events.dispatch(MinionCloseEvent(viewer, minion, MinionMenuType.MAIN))
            }
        }.open(player)
    }

    private fun MenuBuilder.setButton(section: Config, key: String, row: Int, column: Int, button: (Config) -> Slot) =
        setSlots(
            section.getSlotLocations(key, listOf(SlotLocation(row = row, column = column))),
            button(section.getSubsection(key))
        )

    private fun emptySlotItem(definition: MinionDefinition): ItemStack =
        if (input.takesInput(definition)) {
            chrome.inputFillerItem(acceptedNames(definition))
        } else {
            chrome.fillerItem()
        }

    private fun acceptedNames(definition: MinionDefinition): List<String> =
        definition.breeder?.run { food.map { storageView.nameOf(it.item) } }
            ?: fuels.all().filter { definition.feeder?.accepts(it) == true }.map { it.displayName }

    private fun context(minion: Minion): TemplateContext =
        TemplateContext.of(MinionPlaceholders.MINION to minion)

    private fun titleItem(minion: Minion, section: Config): ItemStack {
        val context = context(minion)
        val item = chrome.buttonItem(section, Material.PLAYER_HEAD)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(messages.line("gui.main.title-item.name", context))
        meta.lore = messages.lines("gui.main.title-item.lore", context)
        item.itemMeta = meta
        return item
    }

    private fun inputSlot(player: Player, minion: Minion, shown: ItemStack, sample: ItemStack?): Slot =
        slot(shown) {
            if (input.takesInput(minion.state.definition)) {
                onLeftClick { event, _, menu ->
                    if (regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
                        when {
                            event.cursor.type != Material.AIR -> deposit(player, minion, event.cursor)
                            sample != null -> input.withdraw(player, minion, sample)
                        }
                        menu.refresh(player)
                    }
                }
            }
        }


    private fun deposit(player: Player, minion: Minion, cursor: ItemStack) {
        when (val result = input.deposit(player, minion, cursor)) {
            MinionInput.Deposit.Refused -> player.sendMessage(messages.line("messages.input-not-accepted"))
            MinionInput.Deposit.Cancelled -> Unit
            is MinionInput.Deposit.Accepted -> shrink(player, cursor, result.moved)
        }
    }

    private fun shrink(player: Player, cursor: ItemStack, moved: Int) {
        if (moved == 0) {
            player.sendMessage(messages.line("messages.input-storage-full"))
        } else {
            val left = cursor.amount - moved
            player.setItemOnCursor(if (left <= 0) null else cursor.clone().apply { amount = left })
        }
    }

    private fun collectAllSlot(player: Player, minion: Minion, section: Config): Slot =
        slot({ _, _ -> collectAllItem(minion, section) }) {
            // Refreshed here as well as on the loop so the storage area empties
            // the moment it is clicked, not on the next tick of the loop.
            onLeftClick { _, _, menu ->
                if (!regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
                    return@onLeftClick
                }
                val collected = collector.collect(player, minion) ?: return@onLeftClick
                if (collected == 0 && minion.usedStorage > 0) {
                    player.sendMessage(messages.line("messages.inventory-full"))
                } else {
                    player.sendMessage(
                        messages.line("messages.collected", TemplateContext.of("count" to collected))
                    )
                }
                menu.refresh(player)
            }
        }

    private fun collectAllItem(minion: Minion, section: Config): ItemStack {
        val context = context(minion)
        return chrome.named(
            chrome.buttonItem(section, Material.HOPPER),
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
    private fun fuelSlot(player: Player, minion: Minion, section: Config): Slot =
        slot({ _, _ -> fuelItem(minion, section) }) {
            onLeftClick { event, _, menu -> deposit(player, minion, event, menu, whole = true) }
            onRightClick { event, _, menu -> deposit(player, minion, event, menu, whole = false) }
            onShiftLeftClick { _, _, menu ->
                if (!regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
                    return@onShiftLeftClick
                }
                announce(player, fuelService.fuelFrom(player, minion, limit = Int.MAX_VALUE))
                menu.refresh(player)
            }
        }

    private fun deposit(player: Player, minion: Minion, event: InventoryClickEvent, menu: Menu, whole: Boolean) {
        if (!regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
            return
        }
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

    private fun fuelItem(minion: Minion, section: Config): ItemStack {
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
            active?.let { Items.lookup(it.itemLookup).item.type.takeIf { type -> type.isItem && !type.isAir } }
                ?.let { ItemStack(it) }
                ?: chrome.buttonItem(section, Material.COAL),
            messages.line("gui.main.fuel.name"),
            lore
        )
    }

    private fun pickupSlot(player: Player, minion: Minion, section: Config): Slot {
        val item = chrome.named(
            chrome.buttonItem(section, Material.BARRIER),
            messages.line("gui.main.pickup.name"),
            messages.lines("gui.main.pickup.lore")
        )

        return slot(item) {
            onLeftClick { event, _ ->
                if (!regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
                    return@onLeftClick
                }
                val picked = when (val result = placement.pickUp(player, minion)) {
                    is PlacementService.PickUpResult.PickedUp -> result.item
                    PlacementService.PickUpResult.NotOwner -> {
                        player.sendMessage(messages.line("messages.not-owner"))
                        return@onLeftClick
                    }
                    PlacementService.PickUpResult.NoIslandPermission -> {
                        player.sendMessage(messages.line("messages.no-island-permission"))
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

    private fun upgradesSlot(player: Player, minion: Minion, section: Config): Slot {
        val item = chrome.named(
            chrome.buttonItem(section, Material.ANVIL),
            messages.line("gui.main.upgrades-button.name"),
            messages.lines("gui.main.upgrades-button.lore")
        )
        return slot(item) {
            onLeftClick { _, _ -> upgradeList.open(player, minion) }
        }
    }

    private fun storedExperienceSlot(player: Player, minion: Minion, section: Config): Slot =
        slot({ _, _ ->
            chrome.named(
                chrome.buttonItem(section, Material.EXPERIENCE_BOTTLE),
                messages.line("gui.main.stored-xp.name", context(minion)),
                messages.lines("gui.main.stored-xp.lore", context(minion))
            )
        }) {
            onLeftClick { _, _, menu ->
                if (!regions.ownsOrWarn(minion.entity, MENU_ACTION)) {
                    return@onLeftClick
                }
                val claimed = collector.claimExperience(player, minion)
                player.sendMessage(
                    if (claimed > 0) {
                        messages.line("messages.claimed-xp", TemplateContext.of("amount" to claimed))
                    } else {
                        messages.line("messages.no-stored-xp")
                    }
                )
                menu.refresh(player)
            }
        }

    private fun limitSlot(minion: Minion): Slot =
        slot({ _, _ ->
            val limited = minion.state.limitedSince != null
            chrome.named(
                if (limited) Material.BARRIER else Material.PAPER,
                messages.line("gui.main.limit.name", context(minion)),
                messages.lines(if (limited) "gui.main.limit.limited-lore" else "gui.main.limit.active-lore", context(minion))
            )
        })

    private fun infoSlot(minion: Minion, section: Config): Slot =
        slot({ _, _ ->
            chrome.named(
                chrome.buttonItem(section, Material.BOOK),
                messages.line("gui.main.info.name"),
                messages.lines("gui.main.info.lore", context(minion))
            )
        })

    private companion object {
        const val TITLE_COLUMN = 5
        const val MILLIS_PER_SECOND = 1000L
        const val MULTIPLIER_FORMAT = "%.2fx"
        const val MENU_ACTION = "Using a minion's menu"
    }
}
