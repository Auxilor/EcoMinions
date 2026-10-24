package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.FeederSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads a feeder's `fuels` filter, dropping ids that name no registered fuel. */
class FeederSettingsLoader(
    private val fuels: FuelService
) {
    fun load(config: Config, context: LoadContext): FeederSettings =
        FeederSettings(
            fuelIds = config.getStrings("fuels").filter { it.isNotBlank() }.mapNotNull { id ->
                fuels.getById(id)?.id ?: context.invalid("fuels", id, "a fuel id")
            }
        )
}
