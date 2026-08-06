package com.exanthiax.ecominions.api.fuel

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.inventory.ItemStack

/** Queries over configured fuels and what is currently burning. */
interface FuelsManager {
    fun all(): Collection<Fuel>

    fun getById(id: String): Fuel?

    /** @return The fuel [item] counts as, or null if it is not a fuel. */
    fun matching(item: ItemStack): Fuel?

    /** @return The fuel burning in [minion], or null if none is. */
    fun activeFuel(minion: Minion): Fuel?

    /** @return Milliseconds left on [minion]'s active fuel, or 0 if none is burning. */
    fun remainingMillis(minion: Minion): Long
}
