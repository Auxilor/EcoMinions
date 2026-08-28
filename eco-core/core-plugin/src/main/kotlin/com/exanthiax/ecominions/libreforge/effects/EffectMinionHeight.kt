package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat

object EffectMinionHeight : MinionStatEffect(
    id = "minion_height",
    stat = MinionStat.HEIGHT,
    argument = "blocks"
) {
    override val description = "Raises how far up a minion scans for target blocks."
    override val categories = setOf("minion")
}
