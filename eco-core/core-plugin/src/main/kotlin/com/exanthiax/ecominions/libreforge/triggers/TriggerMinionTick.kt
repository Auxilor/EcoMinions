package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionTick : Trigger("minion_tick") {
    override val description = "Fires every tick an active minion runs."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods.",
        "Fires very often. For something on a timer, use minion_static_<ticks> instead, as it only fires on the ticks it is due."
    )
    override val parameterDescriptions = mapOf(
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
