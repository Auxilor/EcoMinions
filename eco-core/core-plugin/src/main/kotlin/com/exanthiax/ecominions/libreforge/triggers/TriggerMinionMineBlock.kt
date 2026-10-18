package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionMineBlock : Trigger("minion_mine_block") {
    override val description = "Fires when a minion breaks a target block, before its drops are stored."

    override val categories = setOf("minion", "world")

    override val additionalInfo = listOf(
        "Only fires for minions. Use it in a minion type's effects, an upgrade's effects, or xp-gain-methods."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.BLOCK to "The block the minion broke.",
        TriggerParameter.EVENT to "An editable drop event over the block's drops.",
        TriggerParameter.PLAYER to "The owning player, if they are online.",
        TriggerParameter.VALUE to "How many blocks this action broke. More than one for stacked plants."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.BLOCK,
        TriggerParameter.EVENT,
        TriggerParameter.LOCATION,
        TriggerParameter.ITEM,
        TriggerParameter.VALUE
    )
}
