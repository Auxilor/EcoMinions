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

    /**
     * Burns [fuel] on [minion], adding to whatever is already burning.
     *
     * More of the same fuel extends the burn rather than restarting it, so
     * topping a minion up never throws away time the player already paid for. A
     * different fuel replaces the burn instead: only one speed multiplier can be
     * live at a time, and the remaining time belongs to the fuel that set it.
     */
    fun apply(minion: Minion, fuel: Fuel, now: Long = System.currentTimeMillis()) {
        val duration = fuel.durationTicks * MILLIS_PER_TICK
        val carried = if (minion.burningFuelId == fuel.id) remainingOf(minion, now) else 0L

        minion.burningFuelId = fuel.id
        minion.burningFuelRemainingMillis = carried + duration
        // An idle minion holds its fuel frozen as a remainder; giving it an
        // absolute expiry here would burn the fuel off while it is not working.
        minion.burningFuelEndsAt = if (minion.state.shutdownTimestamp != null) {
            0L
        } else {
            now + carried + duration
        }
        repository.save(minion)
    }

    /** @return What is left of the current burn, live or frozen. */
    private fun remainingOf(minion: Minion, now: Long): Long =
        if (minion.burningFuelEndsAt > 0L) {
            (minion.burningFuelEndsAt - now).coerceAtLeast(0L)
        } else {
            minion.burningFuelRemainingMillis.coerceAtLeast(0L)
        }
}
