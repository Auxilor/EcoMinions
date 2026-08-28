package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionCollect : Trigger("minion_collect") {
    override val description = "Fires once per item actually stored in a minion's storage - " +
        "useful for leveling collections or skills off what a minion gathers."

    override val categories = setOf("minion", "inventory")

    override val parameterDescriptions = mapOf(
        TriggerParameter.ITEM to "The item that was stored.",
        TriggerParameter.PLAYER to "The owning player, if they are online.",
        TriggerParameter.VALUE to "How many of the item were stored."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.ITEM,
        TriggerParameter.LOCATION,
        TriggerParameter.VALUE
    )
}
