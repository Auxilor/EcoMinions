package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionFeed : Trigger("minion_feed") {
    override val description = "Fires when a feeder minion refuels another minion."

    override val categories = setOf("minion")

    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.PLAYER to "The owning player, if they are online.",
        TriggerParameter.LOCATION to "The minion that was fed.",
        TriggerParameter.VALUE to "Always 1, for the one minion fed."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.VALUE
    )
}
