package com.exanthiax.ecominions.libreforge.triggers

import com.exanthiax.ecominions.api.event.MinionPickupEvent
import com.exanthiax.ecominions.libreforge.dispatchTrigger
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

object TriggerMinionPickup : Trigger("minion_pickup") {
    override val description = "Fires when a minion is picked up out of the world."
    override val categories = setOf("minion")
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)

    @EventHandler(priority = EventPriority.MONITOR)
    fun handle(event: MinionPickupEvent) {
        (event.minion as? Minion)?.dispatchTrigger(this)
    }
}
