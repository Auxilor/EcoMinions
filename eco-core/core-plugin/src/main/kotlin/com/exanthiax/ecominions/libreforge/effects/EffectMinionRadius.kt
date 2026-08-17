package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat

object EffectMinionRadius : MinionStatEffect(
    id = "minion_radius",
    stat = MinionStat.RADIUS,
    argument = "blocks"
) {
    override val description = "Widens the area a minion scans for target blocks."
    override val categories = setOf("minion")
}
