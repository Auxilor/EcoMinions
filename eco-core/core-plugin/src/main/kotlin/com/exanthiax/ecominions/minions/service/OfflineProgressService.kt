package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.OfflineProgress
import org.bukkit.inventory.ItemStack

/** Credits a minion for the time it spent idle, and burns the fuel that took. */
class OfflineProgressService(
    private val settings: SettingsHolder<MinionSettings>,
    private val fuels: FuelService
) {
    /**
     * @param idleSince When the minion went idle.
     * @param now The current wall-clock time.
     */
    fun apply(minion: Minion, idleSince: Long, now: Long) {
        val sampleBlock = minion.state.type.targetBlocks.firstOrNull() ?: return

        val result = OfflineProgress.simulate(
            elapsedMillis = now - idleSince,
            efficiency = settings.value.offlineEfficiency,
            stats = minion.stats,
            fuelRemainingMillis = minion.burningFuelRemainingMillis,
            fueledCycleTicks = fueledCycleTicksOf(minion)
        ) {
            if (minion.isStorageFull) {
                return@simulate false
            }
            minion.storage.addItem(ItemStack(sampleBlock, 1))
            true
        }

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
}
