package com.exanthiax.ecominions.fuel.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.fuel.model.Fuel
import com.willfp.eco.core.config.interfaces.Config

/** Parses one file in `fuels/` into a [Fuel]. */
class FuelLoader : ConfigLoader<Fuel> {

    override fun load(id: String, config: Config, context: LoadContext): Fuel? {
        val itemLookup = config.getStringOrNull("item")
        if (itemLookup.isNullOrBlank()) {
            context.violation("item", "no item configured - the fuel could never be consumed")
            return null
        }

        val duration = config.getIntOrNull("duration-ticks")?.toLong() ?: DEFAULT_DURATION_TICKS
        if (duration <= 0) {
            context.violation("duration-ticks", "must be positive, was $duration")
            return null
        }

        val multiplier = config.getDoubleOrNull("speed-multiplier") ?: DEFAULT_MULTIPLIER
        if (multiplier <= 0.0) {
            context.violation("speed-multiplier", "must be positive, was $multiplier")
            return null
        }

        return Fuel(
            id = id,
            displayName = config.getFormattedString("name"),
            itemLookup = itemLookup,
            durationTicks = duration,
            speedMultiplier = multiplier
        )
    }

    private companion object {
        const val DEFAULT_DURATION_TICKS = 12000L
        const val DEFAULT_MULTIPLIER = 1.25
    }
}
