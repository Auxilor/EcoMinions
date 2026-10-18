package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionStorageFull : Trigger("minion_storage_full") {
    override val description = "Fires each time a minion tries to mine but its storage is full."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )
    override val parameterDescriptions = mapOf(
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
