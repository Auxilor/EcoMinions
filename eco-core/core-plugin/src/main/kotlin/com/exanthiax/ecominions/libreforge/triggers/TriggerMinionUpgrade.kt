package com.exanthiax.ecominions.libreforge.triggers

import com.exanthiax.ecominions.api.event.MinionUpgradeEvent
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

object TriggerMinionUpgrade : Trigger("minion_upgrade") {
    override val description = "Fires when a player buys an upgrade tier for a minion."
    override val categories = setOf("minion")
    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The id of the upgrade that was bought.",
        TriggerParameter.VALUE to "The tier that was bought."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT,
        TriggerParameter.VALUE
    )

    @EventHandler(priority = EventPriority.MONITOR)
    fun handle(event: MinionUpgradeEvent) {
        val minion = event.minion as? Minion ?: return

        this.dispatch(
            minion.toDispatcher(),
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                location = minion.location,
                text = event.upgrade.id,
                value = event.tier.toDouble()
            )
        )
    }
}
