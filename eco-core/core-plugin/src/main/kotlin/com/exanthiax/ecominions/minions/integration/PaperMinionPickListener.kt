package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.service.MinionService
import io.papermc.paper.event.player.PlayerPickEntityEvent
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import wiki.ecohub.core.plugin.startup.Prerequisite

/**
 * Server-side pick-block on Paper and forks (Purpur, Folia, ...). Kept apart from [MinionPickListener]
 * so Spigot never loads [PlayerPickEntityEvent]; only register when [isSupported].
 */
class PaperMinionPickListener(
    private val minions: MinionService
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPick(event: PlayerPickEntityEvent) {
        val stand = event.entity as? ArmorStand ?: return
        minions.getByEntity(stand) ?: return

        event.isCancelled = true
    }

    companion object {
        /** Paper added [PlayerPickEntityEvent] in 1.21.5. */
        val isSupported: Boolean
            get() = Prerequisite.HAS_PAPER.isMet && Prerequisite.hasVersion(1, 21, 5).isMet
    }
}
