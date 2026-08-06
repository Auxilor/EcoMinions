package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.model.MenuSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.upgrades.ui.UpgradeListMenu
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** The menu shown when a player clicks a minion. */
class MainMenu(
    private val settings: SettingsHolder<MenuSettings>,
    private val messages: Messages,
    private val chrome: MenuChrome,
    private val storageView: MinionStorageView,
    private val placement: PlacementService,
    private val fuels: FuelService,
    private val fuelService: MinionFuelService,
    private val repository: MinionStateRepository,
    private val upgradeList: UpgradeListMenu
) {
    fun open(player: Player, minion: Minion) {
        val current = settings.value
        val rows = current.mainRows
        val context = TemplateContext.of(MinionPlaceholders.MINION to minion)

        menu(rows) {
            setTitle(messages.render(current.mainTitle, context))
            setMask(chrome.fillerMask(rows))

            setSlot(1, TITLE_COLUMN, titleSlot(minion, context))

            val positions = storagePositions(rows)
            positions.zip(storageView.slotsFor(minion, current, positions.size))
                .forEach { (position, slot) -> setSlot(position.first, position.second, slot) }

            setSlot(rows, 2, collectAllSlot(player, minion, context))
            setSlot(rows, 4, fuelSlot(player, minion))
            setSlot(rows, 5, pickupSlot(player, minion))
            setSlot(rows, 7, upgradesSlot(player, minion))
            setSlot(rows, 8, infoSlot(context))
        }.open(player)
    }

    /** The storage area is everything between the title row and the button row. */
    private fun storagePositions(rows: Int): List<Pair<Int, Int>> =
        (2 until rows).flatMap { row -> (2..8).map { column -> row to column } }

    private fun titleSlot(minion: Minion, context: TemplateContext): Slot {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta ?: return slot(item)
        meta.setDisplayName(messages.line("gui.main.title-item.name", context))
        meta.lore = messages.lines("gui.main.title-item.lore", context)
        item.itemMeta = meta
        return slot(item)
    }

    private fun collectAllSlot(player: Player, minion: Minion, context: TemplateContext): Slot {
        val item = chrome.named(
            Material.HOPPER,
            messages.line("gui.main.collect-all.name", context),
            messages.lines("gui.main.collect-all.lore", context)
        )

        return slot(item) {
            onLeftClick { _, _ ->
                val collected = collectInto(player, minion)
                repository.save(minion)
                player.sendMessage(
                    messages.line("messages.collected", TemplateContext.of("count" to collected))
                )
                open(player, minion)
            }
        }
    }

    /** @return How many items made it into the player's inventory. */
    private fun collectInto(player: Player, minion: Minion): Int {
        var collected = 0
        for (index in 0 until minion.storage.size) {
            val item = minion.storage.getItem(index) ?: continue
            val leftover = player.inventory.addItem(item)
            if (leftover.isEmpty()) {
                collected += item.amount
                minion.storage.setItem(index, null)
            } else {
                val remaining = leftover.values.first()
                collected += item.amount - remaining.amount
                minion.storage.setItem(index, remaining)
                break
            }
        }
        return collected
    }

    private fun fuelSlot(player: Player, minion: Minion): Slot {
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

        val item = chrome.named(
            if (active != null) Material.BLAZE_ROD else Material.COAL,
            messages.line("gui.main.fuel.name"),
            lore
        )

        return slot(item) {
            onLeftClick { _, _ ->
                val applied = fuelService.fuelFrom(player, minion)
                if (applied == null) {
                    player.sendMessage(messages.line("messages.no-fuel"))
                } else {
                    player.sendMessage(
                        messages.line(
                            "messages.fuel-applied",
                            TemplateContext.of("fuel_name" to applied.displayName)
                        )
                    )
                }
                open(player, minion)
            }
        }
    }

    private fun pickupSlot(player: Player, minion: Minion): Slot {
        val item = chrome.named(
            Material.BARRIER,
            messages.line("gui.main.pickup.name"),
            messages.lines("gui.main.pickup.lore")
        )

        return slot(item) {
            onLeftClick { event, _ ->
                val picked = placement.pickUp(player, minion)
                if (picked == null) {
                    player.sendMessage(messages.line("messages.not-owner"))
                    return@onLeftClick
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

    private fun infoSlot(context: TemplateContext): Slot =
        slot(
            chrome.named(
                Material.BOOK,
                messages.line("gui.main.info.name"),
                messages.lines("gui.main.info.lore", context)
            )
        )

    private companion object {
        const val TITLE_COLUMN = 5
        const val MILLIS_PER_SECOND = 1000L
        const val MULTIPLIER_FORMAT = "%.2fx"
    }
}
