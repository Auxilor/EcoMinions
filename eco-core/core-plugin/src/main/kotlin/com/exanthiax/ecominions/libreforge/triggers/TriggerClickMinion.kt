package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerClickMinion : Trigger("click_minion") {
    override val description = "Fires when a player shift-right-clicks a minion they can open."

    override val categories = setOf("minion", "interaction")

    override val additionalInfo = listOf(
        "Fires for the player, not the minion, so it works in a player's own effects, such as an EcoItems item.",
        "Add the minion_as_dispatcher mutator to run minion effects, like give_minion_xp, on the clicked minion.",
        "Only the owner, or a player with ecominions.admin, fires it. A plain right-click still opens the menu."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.PLAYER to "The player who clicked.",
        TriggerParameter.VICTIM to "The minion's armour stand.",
        TriggerParameter.LOCATION to "The minion's location."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.VICTIM,
        TriggerParameter.LOCATION,
        TriggerParameter.EVENT
    )
}
