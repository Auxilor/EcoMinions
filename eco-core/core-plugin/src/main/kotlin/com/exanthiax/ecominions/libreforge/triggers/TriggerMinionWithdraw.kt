package com.exanthiax.ecominions.libreforge.triggers

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object TriggerMinionWithdraw : Trigger("minion_withdraw") {
    override val description = "Fires each time a player takes a stack of items out of a minion's storage."

    override val categories = setOf("minion", "inventory")

    override val additionalInfo = listOf(
        "Fires for the player, not the minion, so it works in a player's own effects and in EcoSkills xp-gain-methods.",
        "Fires for collect all, a stack taken in the menu, and API collects.",
        "Does not fire for drop all, because dropped items can be picked up again.",
        "Only counts what reached the player. Items that did not fit stay in storage and are not counted.",
        "Feeders and breeders hold items players put in, and collectors pick up items players throw.",
        "Add not_minion_types: [feeder, breeder, collector] to skip them.",
        "Add the minion_as_dispatcher mutator to run minion effects on the minion the items came from."
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.PLAYER to "The player who took the items.",
        TriggerParameter.ITEM to "The items taken.",
        TriggerParameter.VALUE to "How many of the item were taken.",
        TriggerParameter.VICTIM to "The minion's armour stand.",
        TriggerParameter.LOCATION to "The minion's location."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.ITEM,
        TriggerParameter.VALUE,
        TriggerParameter.VICTIM,
        TriggerParameter.LOCATION
    )
}

/**
 * Dispatches [TriggerMinionWithdraw] for [player] taking [item] out of [minion]; [item]'s amount is what reached them.
 *
 * Top level so the plugin can hold a reference to it before libreforge is on the classpath.
 */
fun dispatchMinionWithdraw(player: Player, minion: Minion, item: ItemStack) {
    TriggerMinionWithdraw.dispatch(
        player.toDispatcher(),
        TriggerData(
            player = player,
            victim = minion.entity,
            location = minion.location,
            item = item,
            value = item.amount.toDouble()
        ),
        allowDuplicates = true
    )
}
