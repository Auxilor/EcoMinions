package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionTick : Trigger("minion_tick") {
    override val description = "Fires every tick an active minion runs."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Fires very frequently - for interval behaviour, prefer `minion_static_<n>` over `every:` here, " +
            "since it skips dispatch entirely on the ticks it doesn't fire."
    )
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
