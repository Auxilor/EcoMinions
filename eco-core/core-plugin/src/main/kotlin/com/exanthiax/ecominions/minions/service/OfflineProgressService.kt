package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionMineBlock
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.OfflineProgress
import com.willfp.libreforge.triggers.event.DropCause
import org.bukkit.inventory.ItemStack

/** Credits a minion for the time it spent idle, and burns the fuel that took. */
class OfflineProgressService(
    private val settings: SettingsHolder<MinionSettings>,
    private val fuels: FuelService,
    private val dropTables: MinionDropTables,
    private val dispatch: MinionTriggerDispatch
) {
    /**
     * @param idleSince When the minion went idle.
     * @param now The current wall-clock time.
     */
    fun apply(minion: Minion, idleSince: Long, now: Long) {
        val targets = minion.state.type.targetBlocks.toList()
        if (targets.isEmpty()) {
            return
        }

        val accumulated = mutableListOf<ItemStack>()

        val result = OfflineProgress.simulate(
            elapsedMillis = now - idleSince,
            efficiency = settings.value.offlineEfficiency,
            stats = minion.stats,
            fuelRemainingMillis = minion.burningFuelRemainingMillis,
            fueledCycleTicks = fueledCycleTicksOf(minion)
        ) {
            if (accumulated.size >= minion.storageSize) {
                return@simulate false
            }
            val material = targets.random()
            accumulated += dropTables.dropsFor(minion.state.type, material, minion.location)
            true
        }

        creditDrops(minion, accumulated)

        minion.burningFuelRemainingMillis =
            (minion.burningFuelRemainingMillis - result.fuelMillisConsumed).coerceAtLeast(0L)
    }

    /**
     * Fuel speed is read against the frozen remainder, so a minion that still has
     * fuel when it went idle mines at the fuelled rate for as long as it lasts.
     */
    private fun fueledCycleTicksOf(minion: Minion): Int {
        val fuel = minion.burningFuelId?.let { fuels.getById(it) } ?: return minion.stats.cycleTicks
        return (fuel.applyTo(minion.stats.speedTicks) + minion.stats.efficiencyTicks).coerceAtLeast(1)
    }

    /**
     * Runs the whole window's drops through one trigger dispatch.
     *
     * One dispatch rather than one per cycle: the walk can credit up to
     * [OfflineProgress.MAX_SIMULATED_CYCLES] cycles on the login tick, and drop
     * effects are per-ItemStack modifiers, so they compose correctly over a batch.
     * The visible trade is that `chance:` rolls once per window - acceptable on a
     * path that is already scaled by offline-efficiency.
     */
    private fun creditDrops(minion: Minion, drops: List<ItemStack>) {
        if (drops.isEmpty()) {
            return
        }

        val results = dispatch.drops(
            minion = minion,
            trigger = TriggerMinionMineBlock,
            drops = drops,
            block = null,
            location = minion.location,
            cause = DropCause.CUSTOM,
            value = drops.size.toDouble()
        )

        minion.storage.store(results.map { (item, _) -> item })
    }
}
