package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

/** Fires once per level a minion gains. Dispatched by EcoMinions, not a Bukkit listener. */
object TriggerMinionLevelUp : Trigger("minion_level_up") {
    override val description = "Fires when a minion levels up."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods.",
        "Can fire while the owner is offline, when catch-up or a timer gives the minion XP."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.VALUE to "The level the minion reached.",
        TriggerParameter.PLAYER to "The owning player, if they are online."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.VALUE
    )
}
