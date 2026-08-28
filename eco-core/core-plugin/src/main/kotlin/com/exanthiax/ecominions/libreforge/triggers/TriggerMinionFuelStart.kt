package com.exanthiax.ecominions.libreforge.triggers

import com.exanthiax.ecominions.api.event.MinionFuelEvent
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

object TriggerMinionFuelStart : Trigger("minion_fuel_start") {
    override val description = "Fires when a minion starts burning a fuel."
    override val categories = setOf("minion")
    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The id of the fuel that started burning."
    )
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT
    )

    @EventHandler(priority = EventPriority.MONITOR)
    fun handle(event: MinionFuelEvent) {
        val minion = event.minion as? Minion ?: return

        this.dispatch(
            minion.toDispatcher(),
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                location = minion.location,
                text = event.fuel.id
            )
        )
    }
}
