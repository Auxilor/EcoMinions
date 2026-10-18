package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionFuelStart : Trigger("minion_fuel_start") {
    override val description = "Fires when a minion starts burning a fuel."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )
    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The id of the fuel that started burning.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT
    )
}
