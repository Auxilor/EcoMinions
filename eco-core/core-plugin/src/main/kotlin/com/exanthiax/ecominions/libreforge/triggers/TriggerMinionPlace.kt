package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionPlace : Trigger("minion_place") {
    override val description = "Fires when a minion is placed in the world."
    override val categories = setOf("minion")
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
