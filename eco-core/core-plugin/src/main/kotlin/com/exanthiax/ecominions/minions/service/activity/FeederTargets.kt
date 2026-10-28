package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity

/**
 * Which minions around a feeder it may refuel.
 *
 * [islandAt] answers null when island privileges are off, so only a shared owner counts then.
 */
class FeederTargets(
    private val entities: WorkAreaEntities,
    private val fuels: FuelService,
    private val minionOf: (Entity) -> Minion?,
    private val isActive: (Minion) -> Boolean,
    private val islandAt: (Location) -> MinionIsland?,
    private val takesInput: (MinionDefinition) -> Boolean
) {
    /** Every minion in [feeder]'s area that it may feed, hungry or not. */
    fun around(feeder: Minion): List<Minion> =
        entities.find(feeder, ArmorStand::class.java)
            .asSequence()
            .mapNotNull(minionOf)
            .filter { it !== feeder && it.state.definition.type != MinionType.FEEDER && isAllowed(feeder, it) }
            .toList()

    fun isHungry(target: Minion): Boolean =
        isActive(target) &&
            fuels.activeFuelOf(target) == null &&
            !(target.burningFuelEndsAt == 0L && target.burningFuelRemainingMillis > 0L) &&
            !(target.isStorageFull && !takesInput(target.state.definition))

    private fun isAllowed(feeder: Minion, target: Minion): Boolean =
        feeder.state.owner == target.state.owner || sharesIsland(feeder, target)

    private fun sharesIsland(feeder: Minion, target: Minion): Boolean {
        val feederIsland = islandAt(feeder.location)
        val targetIsland = islandAt(target.location)
        return feederIsland != null && targetIsland != null &&
            feederIsland.provider == targetIsland.provider && feederIsland.id == targetIsland.id
    }
}
