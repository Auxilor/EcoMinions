package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.islands.MinionAccess
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.entity.Player

/** @return The message telling [player] why they cannot use [minion]. */
fun Messages.useDenial(access: MinionAccess, player: Player, minion: Minion): String =
    if (access.deniedByIsland(player, minion, IslandPrivilege.USE)) {
        line("messages.no-island-permission")
    } else {
        line("messages.not-owner")
    }
