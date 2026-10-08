package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.menu.MenusManager
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.ui.MainMenu
import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import com.exanthiax.ecominions.upgrades.ui.UpgradeListMenu
import org.bukkit.entity.Player
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion

/**
 * The public menus API. The menus fire their own open and close events.
 *
 * Each menu opens on the player's own region.
 */
class MenusManagerImpl(
    private val mainMenu: MainMenu,
    private val upgradeListMenu: UpgradeListMenu,
    private val typeSelectMenu: TypeSelectMenu,
    private val minions: MinionService,
    private val regions: Regions
) : MenusManager {

    override fun openMain(player: Player, minion: ApiMinion) {
        val resolved = resolve(minion)
        regions.on(player) { mainMenu.open(player, resolved) }
    }

    override fun openUpgrades(player: Player, minion: ApiMinion) {
        val resolved = resolve(minion)
        regions.on(player) { upgradeListMenu.open(player, resolved) }
    }

    override fun openTypeSelect(player: Player) {
        regions.on(player) { typeSelectMenu.open(player) }
    }

    override fun close(player: Player) {
        regions.on(player) { player.closeInventory() }
    }

    private fun resolve(minion: ApiMinion): Minion =
        minions.getByUuid(minion.uuid)
            ?: throw IllegalArgumentException("Minion ${minion.uuid} is not loaded")
}
