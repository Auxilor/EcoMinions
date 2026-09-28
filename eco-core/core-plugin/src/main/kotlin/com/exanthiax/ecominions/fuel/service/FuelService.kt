package com.exanthiax.ecominions.fuel.service

import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.fuel.model.Fuel
import com.willfp.eco.core.items.Items
import org.bukkit.inventory.ItemStack
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion

/**
 * Owns fuel lookups and burn state queries.
 *
 * Knows nothing about minion internals: callers pass the burning fuel's id and
 * expiry, which is all the minions slice stores.
 */
class FuelService(
    private val fuels: ConfigDrivenRegistry<Fuel>
) {

    fun all(): Collection<Fuel> = fuels.all()

    fun getById(id: String): Fuel? = fuels.find(id)

    fun matching(item: ItemStack): Fuel? =
        fuels.all().firstOrNull { Items.lookup(it.itemLookup).matches(item) }

    fun activeFuel(minion: ApiMinion): Fuel? = activeFuelOf(minion)

    fun remainingMillis(minion: ApiMinion): Long {
        val state = burnStateOf(minion) ?: return 0L
        return (state.second - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /** @return The burning fuel, expiring it if its time has passed. */
    fun activeFuelOf(minion: ApiMinion): Fuel? {
        val (id, _) = burnStateOf(minion) ?: return null
        return getById(id)
    }

    /** @return Ticks between mining actions with any active fuel applied. */
    fun effectiveSpeedTicks(minion: ApiMinion, baseTicks: Int): Int =
        activeFuelOf(minion)?.applyTo(baseTicks) ?: baseTicks

    private fun burnStateOf(minion: ApiMinion): Pair<String, Long>? {
        val holder = minion as? FuelBurner ?: return null
        val id = holder.burningFuelId ?: return null
        if (System.currentTimeMillis() >= holder.burningFuelEndsAt) {
            return null
        }
        return id to holder.burningFuelEndsAt
    }

    /**
     * Implemented by whatever can burn fuel, so this slice never imports the
     * minions slice to read burn state.
     */
    interface FuelBurner {
        val burningFuelId: String?
        val burningFuelEndsAt: Long
    }
}
