package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionUpgrade : Trigger("minion_upgrade") {
    override val description = "Fires when a player buys an upgrade tier for a minion."
    override val categories = setOf("minion")
    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The id of the upgrade that was bought.",
        TriggerParameter.VALUE to "The tier that was bought."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT,
        TriggerParameter.VALUE
    )
}
