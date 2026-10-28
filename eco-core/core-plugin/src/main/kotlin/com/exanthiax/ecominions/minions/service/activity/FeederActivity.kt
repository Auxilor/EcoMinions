package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionFeedEvent
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFeed
import com.exanthiax.ecominions.minions.model.FeederSettings
import com.exanthiax.ecominions.minions.model.accepts
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionFuelService
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * Refuels one unfuelled minion in its box per action, from fuel stored in itself.
 *
 * Earns nothing offline: feeding needs the neighbours to be working, and nobody is there to load fuel.
 */
class FeederActivity(
    private val targets: FeederTargets,
    private val fuels: FuelService,
    private val fuelService: MinionFuelService,
    private val storageSaved: (Minion) -> Unit,
    private val signals: ActivitySignals,
    private val canWork: (Minion, Block) -> Boolean = { _, _ -> true }
) : MinionActivity {

    override fun act(minion: Minion, now: Long): ActivityResult {
        val settings = minion.state.definition.feeder
        return when {
            settings == null -> {
                ActivityResult.BLOCKED
            }
            storedFuels(minion, settings).isEmpty() -> {
                minion.blockedReason = NO_FUEL_STORED
                ActivityResult.BLOCKED
            }
            feedFirst(minion, settings) -> {
                ActivityResult.WORKED
            }
            else -> {
                minion.blockedReason = NOTHING_TO_FEED
                ActivityResult.BUSY
            }
        }
    }

    override fun hasWork(minion: Minion): Boolean = true

    override fun offlineCycle(minion: Minion): OfflineYield? = null

    private fun feedFirst(minion: Minion, settings: FeederSettings): Boolean {
        for (target in targets.around(minion)) {
            if (canWork(minion, target.location.block) && targets.isHungry(target) && feed(minion, target, settings)) {
                return true
            }
        }
        return false
    }

    private fun storedFuels(minion: Minion, settings: FeederSettings): List<Pair<ItemStack, Fuel>> =
        (0 until minion.storage.size).mapNotNull { index -> storedFuel(minion.storage.getItem(index), settings) }

    private fun storedFuel(stack: ItemStack?, settings: FeederSettings): Pair<ItemStack, Fuel>? {
        val fuel = stack?.let(fuels::matching)?.takeIf(settings::accepts)
        return if (stack == null || fuel == null) null else stack to fuel
    }

    private fun feed(minion: Minion, target: Minion, settings: FeederSettings): Boolean {
        val stored = storedFuels(minion, settings)
        val chosen = stored.firstOrNull { (_, candidate) -> candidate.id == target.burningFuelId }
            ?: stored.firstOrNull()
        return chosen != null && consume(minion, target, chosen.first, chosen.second)
    }

    private fun consume(minion: Minion, target: Minion, stack: ItemStack, fuel: Fuel): Boolean {
        val event = MinionFeedEvent(
            minion = minion,
            target = target,
            fuel = fuel,
            durationMillis = fuelService.unitMillis(fuel)
        )
        return signals.allows(event, MinionFeedEvent.getHandlerList()) &&
            event.durationMillis > 0L &&
            take(minion, target, stack, fuel, event.durationMillis)
    }

    private fun take(minion: Minion, target: Minion, stack: ItemStack, fuel: Fuel, durationMillis: Long): Boolean {
        val single = stack.clone().apply { amount = 1 }
        val removed = minion.storage.remove(single) > 0
        val applied = removed && fuelService.applyDirect(target, fuel, durationMillis)
        if (removed && !applied) {
            minion.storage.add(single)
        }
        if (removed) {
            storageSaved(minion)
        }
        if (applied) {
            signals.trigger(minion, TriggerMinionFeed, target.location, 1.0)
        }
        return applied
    }

    private companion object {
        const val NOTHING_TO_FEED = "nothing-to-feed"
        const val NO_FUEL_STORED = "no-fuel-stored"
    }
}
