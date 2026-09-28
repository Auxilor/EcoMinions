package com.exanthiax.ecominions.api.fuel

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Configured fuels, and what minions are burning. */
interface FuelsManager {
    fun all(): Collection<Fuel>

    fun getById(id: String): Fuel?

    /** @return The fuel [item] counts as, or null if it is not a fuel. */
    fun matching(item: ItemStack): Fuel?

    /** @return The fuel burning in [minion], or null if none is. */
    fun activeFuel(minion: Minion): Fuel?

    /** @return Milliseconds left on [minion]'s active fuel, or 0 if none is burning. */
    fun remainingMillis(minion: Minion): Long

    /**
     * Burns one unit of [fuel] on [minion] without taking any item.
     * Fires `MinionFuelEvent` with no player.
     *
     * @return Whether the fuel was applied.
     */
    fun applyFuel(minion: Minion, fuel: Fuel): Boolean

    /**
     * Burns [fuel] on [minion] for [durationMillis] without taking any item.
     * Fires `MinionFuelEvent` with no player.
     *
     * More of the fuel already burning adds to the time left; a different fuel
     * replaces it.
     *
     * @return Whether the fuel was applied.
     */
    fun applyFuel(minion: Minion, fuel: Fuel, durationMillis: Long): Boolean

    /**
     * Takes one fuel item from [player]'s inventory and burns it on [minion], as
     * the fuel button does. Fires `MinionFuelEvent`.
     *
     * @return The fuel burnt, or null if the player had none or the event was cancelled.
     */
    fun applyFrom(player: Player, minion: Minion): Fuel?

    /** Puts out whatever [minion] is burning. */
    fun clearFuel(minion: Minion)
}
