package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionFeedEvent
import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFeed
import com.exanthiax.ecominions.minions.model.FeederSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.accepts
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * Refuels one unfuelled minion in its box per action, from fuel stored in itself.
 *
 * Earns nothing offline: feeding needs the neighbours to be working, and nobody is there to load fuel.
 */
class FeederActivity(
    private val entities: WorkAreaEntities,
    private val fuels: FuelService,
    private val fuelService: MinionFuelService,
    private val storageSaved: (Minion) -> Unit,
    private val minionOf: (Entity) -> Minion?,
    private val isActive: (Minion) -> Boolean,
    private val islandAt: (Location) -> MinionIsland?,
    private val islandPrivileges: () -> Boolean,
    private val takesInput: (MinionDefinition) -> Boolean,
    private val events: EventDispatcher,
    private val dispatch: MinionTriggerDispatch,
    private val listening: (HandlerList) -> Boolean = { it.registeredListeners.isNotEmpty() },
    private val canWork: (Minion, Block) -> Boolean = { _, _ -> true }
) : MinionActivity {

    override fun act(minion: Minion, now: Long): ActivityResult {
        val settings = minion.state.definition.feeder
        return when {
            settings == null -> ActivityResult.BLOCKED
            storedFuels(minion, settings).isEmpty() -> {
                minion.blockedReason = NO_FUEL_STORED
                ActivityResult.BLOCKED
            }
            feedFirst(minion, settings) -> ActivityResult.WORKED
            else -> {
                minion.blockedReason = NOTHING_TO_FEED
                ActivityResult.BUSY
            }
        }
    }

    override fun hasWork(minion: Minion): Boolean = true

    override fun offlineCycle(minion: Minion): OfflineYield? = null

    private fun feedFirst(minion: Minion, settings: FeederSettings): Boolean {
        for (target in neighbours(minion).filter(::isHungry)) {
            if (feed(minion, target, settings)) {
                return true
            }
        }
        return false
    }

    private fun neighbours(minion: Minion): List<Minion> =
        entities.find(minion, ArmorStand::class.java)
            .mapNotNull(minionOf)
            .filter { it !== minion && it.state.definition.type != MinionType.FEEDER }
            .filter { canWork(minion, it.location.block) && isAllowed(minion, it) }

    private fun isAllowed(minion: Minion, target: Minion): Boolean =
        minion.state.owner == target.state.owner || (islandPrivileges() && sharesIsland(minion, target))

    private fun sharesIsland(minion: Minion, target: Minion): Boolean {
        val feederIsland = islandAt(minion.location)
        val targetIsland = islandAt(target.location)
        return feederIsland != null && targetIsland != null &&
            feederIsland.provider == targetIsland.provider && feederIsland.id == targetIsland.id
    }

    private fun isHungry(target: Minion): Boolean =
        isActive(target) &&
            fuels.activeFuelOf(target) == null &&
            !(target.burningFuelEndsAt == 0L && target.burningFuelRemainingMillis > 0L) &&
            !(target.isStorageFull && !takesInput(target.state.definition))

    private fun storedFuels(minion: Minion, settings: FeederSettings): List<Pair<ItemStack, Fuel>> =
        (0 until minion.storage.size).mapNotNull { index ->
            val stack = minion.storage.getItem(index) ?: return@mapNotNull null
            val fuel = fuels.matching(stack)?.takeIf(settings::accepts) ?: return@mapNotNull null
            stack to fuel
        }

    private fun feed(minion: Minion, target: Minion, settings: FeederSettings): Boolean {
        val stored = storedFuels(minion, settings)
        val chosen = stored.firstOrNull { (_, candidate) -> candidate.id == target.burningFuelId }
            ?: stored.firstOrNull()
        return chosen != null && consume(minion, target, chosen.first, chosen.second)
    }

    private fun consume(minion: Minion, target: Minion, stack: ItemStack, fuel: Fuel): Boolean {
        val event = MinionFeedEvent(minion, target, fuel, fuelService.unitMillis(fuel))
        val allowed = !listening(MinionFeedEvent.getHandlerList()) || events.dispatch(event)
        return allowed && event.durationMillis > 0L && take(minion, target, stack, fuel, event.durationMillis)
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
            dispatch.triggerAt(minion, TriggerMinionFeed, target.location, 1.0)
        }
        return applied
    }

    private companion object {
        const val NOTHING_TO_FEED = "nothing-to-feed"
        const val NO_FUEL_STORED = "no-fuel-stored"
    }
}
