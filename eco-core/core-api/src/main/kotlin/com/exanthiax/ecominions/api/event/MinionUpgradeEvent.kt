package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion
import com.exanthiax.ecominions.api.upgrade.Upgrade
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired after an upgrade tier has been bought and applied. */
class MinionUpgradeEvent(
    val minion: Minion,
    val player: Player,
    val upgrade: Upgrade,
    val tier: Int
) : Event() {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
