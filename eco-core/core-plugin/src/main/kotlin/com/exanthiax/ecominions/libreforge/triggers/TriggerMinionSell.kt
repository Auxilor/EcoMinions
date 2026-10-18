package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionSell : Trigger("minion_sell") {
    override val description = "Fires when a minion's storage is sold."
    override val categories = setOf("minion", "economy")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )
    override val parameterDescriptions = mapOf(
        TriggerParameter.VALUE to "The money the sale earned.",
        TriggerParameter.EVENT to "An editable drop event over the stacks being sold.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.EVENT,
        TriggerParameter.VALUE
    )
}
