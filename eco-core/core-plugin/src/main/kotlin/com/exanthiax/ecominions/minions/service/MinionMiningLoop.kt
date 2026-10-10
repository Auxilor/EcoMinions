package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.lifecycle.Tickable
import com.exanthiax.ecominions.core.scheduling.Regions
import org.bukkit.Bukkit

/**
 * Ticks every live minion from one repeating task, rather than
 * scheduling four tasks per placed minion.
 *
 * The loop itself runs on the global region; each minion's mining is handed to
 * the region that owns its stand, and the tick is read once here so every minion
 * is measured against the same clock.
 */
class MinionMiningLoop(
    private val minions: MinionService,
    private val ticker: MinionTicker,
    private val activity: MinionActivityService,
    private val regions: Regions
) : Tickable {

    override val tickInterval = 1L

    override fun tick() {
        val now = Bukkit.getCurrentTick().toLong()
        for (minion in minions.all()) {
            if (!activity.isActive(minion)) {
                continue
            }
            regions.on(minion.entity) { ticker.tick(minion, now) }
        }
    }
}
