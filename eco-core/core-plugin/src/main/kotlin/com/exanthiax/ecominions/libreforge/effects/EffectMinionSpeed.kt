package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.arguments

object EffectMinionSpeed : MinionStatEffect(
    id = "minion_speed",
    stat = MinionStat.SPEED_TICKS,
    argument = "ticks_faster",
    sign = -1
) {
    override val description = "Reduces the ticks a minion waits between actions."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions: in a minion type's effects or an upgrade's effects. Anywhere else it does nothing.",
        "Permanent, so it needs no triggers. It stacks with every other effect on the same stat."
    )
    override val arguments = arguments {
        require(
            "ticks_faster",
            "You must specify ticks_faster!",
            description = "How many fewer ticks the minion waits between actions.",
            type = ArgType.EXPRESSION,
            example = "%tier_value%"
        )
    }
}
