package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * The owning player, when they are online.
 *
 * Null otherwise, which is what stock player-targeting effects expect - they
 * no-op rather than misfire. A minion's behaviour therefore differs between an
 * online and offline owner, which is documented rather than hidden.
 */
fun Minion.ownerPlayer(): Player? = state.owner?.let { Bukkit.getPlayer(it) }
