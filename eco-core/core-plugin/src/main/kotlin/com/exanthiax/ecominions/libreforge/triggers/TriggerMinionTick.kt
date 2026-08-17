package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerParameter

object TriggerMinionTick : Trigger("minion_tick") {
    override val description =
        "Fires every tick an active minion runs. Pair with `every:` on the effect block for interval behaviour."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Fires very frequently. Always constrain it with `every:` or a condition."
    )
    override val parameters = setOf(TriggerParameter.PLAYER, TriggerParameter.LOCATION)
}
