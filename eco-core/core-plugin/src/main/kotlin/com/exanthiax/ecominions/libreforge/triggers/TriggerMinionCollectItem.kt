package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionCollectItem : Trigger("minion_collect_item") {
    override val description = "Fires when a collector minion picks up a dropped item."

    override val categories = setOf("minion")

    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.ITEM to "The item picked up.",
        TriggerParameter.PLAYER to "The owning player, if they are online.",
        TriggerParameter.VALUE to "How many items were stored."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.ITEM,
        TriggerParameter.LOCATION,
        TriggerParameter.VALUE
    )
}
