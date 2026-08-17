package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionStorageFull : Trigger("minion_storage_full") {
    override val description = "Fires each time a minion tries to mine but its storage is full."
    override val categories = setOf("minion")
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
