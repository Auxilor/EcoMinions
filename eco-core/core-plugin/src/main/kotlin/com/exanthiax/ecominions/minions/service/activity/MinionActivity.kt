package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionTicker
import org.bukkit.inventory.ItemStack

/**
 * What one [MinionType] does with its turn.
 *
 * [MinionTicker] owns everything every type shares: fuel, triggers, storage and
 * the cadence. An activity only decides what this minion does now.
 */
interface MinionActivity {
    /** Runs every tick, before the cadence check, for work that lands between actions. */
    fun onTick(minion: Minion, now: Long) = Unit

    fun act(minion: Minion, now: Long): ActivityResult

    /** @return true if the minion has anything to do, or could make its own work. */
    fun hasWork(minion: Minion): Boolean

    /** @return What one offline cycle earns, or null to end the offline walk. */
    fun offlineCycle(minion: Minion): OfflineYield?
}

enum class ActivityResult {
    WORKED,
    SEEDED,
    BUSY,
    BLOCKED
}

data class OfflineYield(val drops: List<ItemStack>, val experience: Int)
