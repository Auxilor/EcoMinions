package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionRegenBlock : Trigger("minion_regen_block") {
    override val description = "Fires when a block a minion mined regenerates."
    override val categories = setOf("minion", "world")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )
    override val parameterDescriptions = mapOf(
        TriggerParameter.BLOCK to "The block that regenerated.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.BLOCK,
        TriggerParameter.LOCATION
    )
}
