package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionBreed : Trigger("minion_breed") {
    override val description = "Fires when a breeder minion breeds a pair of animals."

    override val categories = setOf("minion")

    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.PLAYER to "The owning player, if they are online.",
        TriggerParameter.LOCATION to "The first animal of the pair.",
        TriggerParameter.VALUE to "How many food items were used."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.VALUE
    )
}
