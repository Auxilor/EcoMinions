package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat

object EffectMinionEfficiency : MinionStatEffect(
    id = "minion_efficiency",
    stat = MinionStat.EFFICIENCY_TICKS,
    argument = "ticks_faster",
    sign = -1
) {
    override val description = "Reduces the ticks a minion spends breaking a block."
    override val categories = setOf("minion")
}
