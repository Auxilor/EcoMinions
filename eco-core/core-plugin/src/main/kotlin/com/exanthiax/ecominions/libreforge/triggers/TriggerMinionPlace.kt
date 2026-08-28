package com.exanthiax.ecominions.libreforge.triggers

import com.exanthiax.ecominions.api.event.MinionPlaceEvent
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class TriggerMinionPlace(private val dispatch: MinionTriggerDispatch) : Trigger("minion_place") {
    override val description = "Fires when a minion is placed in the world."
    override val categories = setOf("minion")
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)

    @EventHandler(priority = EventPriority.MONITOR)
    fun handle(event: MinionPlaceEvent) {
        (event.minion as? Minion)?.let { dispatch.trigger(it, this) }
    }
}
