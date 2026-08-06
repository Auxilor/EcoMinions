package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.lifecycle.Tickable
import org.bukkit.Bukkit

/**
 * Ticks mining for every live minion from one repeating task, rather than
 * scheduling four tasks per placed minion.
 */
class MinionMiningLoop(
    private val minions: MinionService,
    private val mining: MiningService
) : Tickable {

    override val tickInterval = 1L

    override fun tick() {
        val now = Bukkit.getCurrentTick().toLong()
        for (minion in minions.all()) {
            mining.tick(minion, now)
        }
    }
}
