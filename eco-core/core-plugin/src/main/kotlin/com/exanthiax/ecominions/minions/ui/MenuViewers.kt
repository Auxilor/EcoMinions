package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.lifecycle.Tickable
import com.willfp.eco.core.gui.menu.Menu
import org.bukkit.entity.Player

/**
 * Re-renders the menus players currently have open.
 *
 * eco renders a menu once when it is opened, so a menu that shows something the
 * server keeps changing - mined loot, fuel burning down - goes stale as soon as
 * it is on screen. Menus register their viewers here and this loop drives
 * [Menu.refresh] for them; the slots themselves have to be provider-backed for
 * that refresh to produce anything new.
 */
class MenuViewers(
    private val refresh: (Menu, Player) -> Unit = { menu, player -> menu.refresh(player) }
) : Tickable {
    override val tickInterval = REFRESH_INTERVAL_TICKS

    private val viewing = mutableMapOf<Player, Menu>()

    /** Start refreshing [menu] for [player], replacing whatever they had open. */
    fun watch(player: Player, menu: Menu) {
        viewing[player] = menu
    }

    /**
     * Stop refreshing [menu] for [player].
     *
     * Opening a menu from another menu closes the old inventory *after* the new
     * one is open, so a close only counts when it is the menu still on screen.
     */
    fun forget(player: Player, menu: Menu) {
        if (viewing[player] === menu) {
            viewing.remove(player)
        }
    }

    override fun tick() {
        val iterator = viewing.entries.iterator()
        while (iterator.hasNext()) {
            val (player, menu) = iterator.next()
            if (!player.isOnline) {
                iterator.remove()
                continue
            }
            refresh(menu, player)
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_TICKS = 10L
    }
}
