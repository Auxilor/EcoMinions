package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionRegenBlock : Trigger("minion_regen_block") {
    override val description = "Fires when a block a minion mined regenerates."
    override val categories = setOf("minion", "world")
    override val parameterDescriptions = mapOf(
        TriggerParameter.BLOCK to "The block that regenerated."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.BLOCK,
        TriggerParameter.LOCATION
    )
}
