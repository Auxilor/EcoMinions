package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.minions.model.FishingSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads a fishing minion's `min-water` and `drops`. */
class FishingSettingsLoader(
    private val dropLists: MinionDropListLoader
) {
    fun load(config: Config, context: LoadContext): FishingSettings {
        val minWater = config.getIntOrNull("min-water") ?: DEFAULT_MIN_WATER
        if (minWater < 1) {
            context.violation("min-water", "$minWater is below 1 - it is raised to 1")
        }
        return FishingSettings(minWater.coerceAtLeast(1), dropLists.load(config.getSubsections("drops"), "drops", context))
    }

    private companion object {
        const val DEFAULT_MIN_WATER = 1
    }
}
