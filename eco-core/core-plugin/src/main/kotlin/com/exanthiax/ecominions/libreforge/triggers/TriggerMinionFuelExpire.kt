package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionFuelExpire : Trigger("minion_fuel_expire") {
    override val description = "Fires on the tick a minion's fuel runs out."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only fires for an active minion. An idle minion's fuel is frozen, so it cannot expire while idle."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION
    )
}
