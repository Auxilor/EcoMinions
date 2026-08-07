package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.DropRoll
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.OfflineProgress
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

/** Credits a minion for the time its chunk spent unloaded. */
class OfflineProgressService(
    private val settings: SettingsHolder<MinionSettings>,
    private val repository: MinionStateRepository
) {
    fun apply(minion: Minion) {
        val shutdownAt = minion.state.shutdownTimestamp ?: return
        val elapsed = System.currentTimeMillis() - shutdownAt

        val cycles = OfflineProgress.simulate(
            elapsedMillis = elapsed,
            efficiency = settings.value.offlineEfficiency,
            stats = minion.stats,
            fuelRemainingMillis = 0L,
            fueledCycleTicks = minion.stats.cycleTicks
        ) { true }.cycles

        if (cycles == 0) {
            repository.clearShutdown(minion)
            return
        }

        val sampleBlock = minion.state.type.targetBlocks.firstOrNull() ?: return
        var simulated = 0
        while (simulated < cycles && !minion.isStorageFull) {
            val amount = DropRoll.amount(1, minion.stats.fortuneMultiplier, ThreadLocalRandom.current().nextDouble())
            if (amount > 0) {
                minion.storage.addItem(ItemStack(sampleBlock, amount))
            }
            simulated++
        }

        repository.clearShutdown(minion)
        minion.state = minion.state.copy(shutdownTimestamp = null)
    }
}
