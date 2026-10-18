package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionCatchFish : Trigger("minion_catch_fish") {
    override val description = "Fires when a fishing minion catches something, before it is stored."

    override val categories = setOf("minion")

    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.BLOCK to "The water block fished in.",
        TriggerParameter.EVENT to "An editable drop event over the catch.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.BLOCK,
        TriggerParameter.EVENT,
        TriggerParameter.LOCATION,
        TriggerParameter.ITEM,
        TriggerParameter.VALUE
    )
}
