package com.exanthiax.ecominions.libreforge.triggers

import wiki.ecohub.rules.trigger.Trigger
import wiki.ecohub.rules.trigger.TriggerParameter

object TriggerMinionUpgrade : Trigger("minion_upgrade") {
    override val description = "Fires when a player buys an upgrade tier for a minion."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )
    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The id of the upgrade that was bought.",
        TriggerParameter.VALUE to "The tier that was bought.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT,
        TriggerParameter.VALUE
    )
}
