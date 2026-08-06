package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionFuelEvent
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Fuelling a minion from a player's inventory. */
class MinionFuelService(
    private val fuels: FuelService,
    private val repository: MinionStateRepository
) {
    private companion object {
        const val MILLIS_PER_TICK = 50L
    }

    /**
     * Consumes one fuel item from [player] and applies it to [minion].
     *
     * @return The fuel that was applied, or null if the player had none.
     */
    fun fuelFrom(player: Player, minion: Minion): Fuel? {
        val contents = player.inventory.contents
        for (slot in contents.indices) {
            val stack = contents[slot] ?: continue
            val fuel = fuels.matching(stack) ?: continue

            stack.amount -= 1
            if (stack.amount <= 0) {
                contents[slot] = null
            }
            player.inventory.contents = contents

            apply(minion, fuel)
            Bukkit.getPluginManager().callEvent(MinionFuelEvent(minion, player, fuel))
            return fuel
        }
        return null
    }

    private fun apply(minion: Minion, fuel: Fuel) {
        minion.burningFuelId = fuel.id
        minion.burningFuelEndsAt = System.currentTimeMillis() + fuel.durationTicks * MILLIS_PER_TICK
        repository.save(minion)
    }
}
