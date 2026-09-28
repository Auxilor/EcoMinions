package com.exanthiax.ecominions.api.menu

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player

/** Opens EcoMinions menus. Minion menus fire `MinionOpenEvent` first. */
interface MenusManager {
    fun openMain(player: Player, minion: Minion)

    fun openUpgrades(player: Player, minion: Minion)

    /** Opens the menu that lists every minion type. Not a minion menu, so it fires no event. */
    fun openTypeSelect(player: Player)

    /** Closes whatever inventory [player] has open. */
    fun close(player: Player)
}
