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
 *
 * On its region each minion is first reconciled, so a minion whose activity changed
 * without an event is resumed or stamped idle before it mines. Only a minion that is
 * idle and already stamped is skipped without a hop.
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
            if (minion.state.shutdownTimestamp != null && !activity.isActive(minion)) {
                continue
            }
            regions.on(minion.entity) {
                if (activity.reconcile(minion)) {
                    ticker.tick(minion, now)
                }
            }
        }
    }
}
