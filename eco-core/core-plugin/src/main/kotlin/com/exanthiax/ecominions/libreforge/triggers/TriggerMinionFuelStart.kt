package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionFuelStart : Trigger("minion_fuel_start") {
    override val description = "Fires when a minion starts burning a fuel."
    override val categories = setOf("minion")
    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The id of the fuel that started burning."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT
    )
}
