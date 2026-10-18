package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.arguments

object EffectMinionRadius : MinionStatEffect(
    id = "minion_radius",
    stat = MinionStat.RADIUS,
    argument = "blocks"
) {
    override val description = "Widens a minion's work area."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions: in a minion type's effects or an upgrade's effects. Anywhere else it does nothing.",
        "Permanent, so it needs no triggers. It stacks with every other effect on the same stat."
    )
    override val arguments = arguments {
        require(
            "blocks",
            "You must specify blocks!",
            description = "How many blocks to add to the radius.",
            type = ArgType.EXPRESSION,
            example = "%tier_value%"
        )
    }
}
