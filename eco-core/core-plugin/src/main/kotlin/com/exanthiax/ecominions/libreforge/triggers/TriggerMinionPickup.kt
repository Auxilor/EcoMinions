package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionPickup : Trigger("minion_pickup") {
    override val description = "Fires when a minion is picked up out of the world."
    override val categories = setOf("minion")
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
