package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat

object EffectMinionSpeed : MinionStatEffect(
    id = "minion_speed",
    stat = MinionStat.SPEED_TICKS,
    argument = "ticks_faster",
    sign = -1
) {
    override val description = "Reduces the ticks between a minion's mine cycles."
    override val categories = setOf("minion")
}
