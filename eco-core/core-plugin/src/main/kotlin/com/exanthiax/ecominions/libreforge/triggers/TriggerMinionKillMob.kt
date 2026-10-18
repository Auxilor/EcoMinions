package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionKillMob : Trigger("minion_kill_mob") {
    override val description = "Fires when a slayer minion kills a mob, before its drops are stored."

    override val categories = setOf("minion", "entity")

    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.VICTIM to "The mob killed.",
        TriggerParameter.EVENT to "An editable drop event over the kill's drops.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.VICTIM,
        TriggerParameter.EVENT,
        TriggerParameter.LOCATION,
        TriggerParameter.ITEM,
        TriggerParameter.VALUE
    )
}
