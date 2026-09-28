package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionCompactService
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.get
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

/** Runs every compactor recipe a minion owns and currently meets the conditions for. */
class EffectMinionCompact(
    private val compacting: MinionCompactService
) : Effect<NoCompileData>("minion_compact") {
    override val description = "Compacts a minion's storage using its purchased compactor recipes."
    override val categories = setOf("minion", "inventory")

    override val parameters = setOf(TriggerParameter.LOCATION)

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: return false
        return compacting.compact(minion)
    }
}
