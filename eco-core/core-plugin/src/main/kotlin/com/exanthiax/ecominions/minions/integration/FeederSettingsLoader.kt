package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.FeederSettings
import com.willfp.eco.core.config.interfaces.Config

/**
 * Reads a feeder's `fuels` filter, dropping ids that name no registered fuel.
 *
 * An empty list means every fuel. A list whose ids are all invalid rejects the minion rather than
 * quietly accepting every fuel.
 */
class FeederSettingsLoader(
    private val fuels: FuelService
) {
    fun load(config: Config, context: LoadContext): FeederSettings? {
        val configured = config.getStrings("fuels").filter { it.isNotBlank() }
        val resolved = configured.mapNotNull { id ->
            fuels.getById(id)?.id ?: context.invalid(param = "fuels", value = id, expected = "a fuel id")
        }
        val rejected = configured.isNotEmpty() && resolved.isEmpty()
        if (rejected) {
            context.violation(param = "fuels", message = "every configured fuel is invalid")
        }
        return if (rejected) null else FeederSettings(fuelIds = resolved)
    }
}
