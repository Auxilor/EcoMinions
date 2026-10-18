package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.arguments

object EffectMinionEfficiency : MinionStatEffect(
    id = "minion_efficiency",
    stat = MinionStat.EFFICIENCY_TICKS,
    argument = "ticks_faster",
    sign = -1
) {
    override val description = "Reduces the ticks each of a minion's actions takes."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions: in a minion type's effects or an upgrade's effects. Anywhere else it does nothing.",
        "Permanent, so it needs no triggers. It stacks with every other effect on the same stat."
    )
    override val arguments = arguments {
        require(
            "ticks_faster",
            "You must specify ticks_faster!",
            description = "How many fewer ticks each action takes.",
            type = ArgType.EXPRESSION,
            example = "%tier_value%"
        )
    }
}
