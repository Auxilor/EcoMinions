package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionCollect : Trigger("minion_collect") {
    override val description = "Fires each time a stack of items is stored in a minion's storage."

    override val categories = setOf("minion", "inventory")

    override val additionalInfo = listOf(
        "Fires for the minion, so use it in a minion type's effects, an upgrade's effects, or xp-gain-methods.",
        "Player effects there, like give_skill_xp or give_money, act on the owner while they are online.",
        "Items that did not fit in a full storage do not count, so it is safe for levelling collections or skills."
    )

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
