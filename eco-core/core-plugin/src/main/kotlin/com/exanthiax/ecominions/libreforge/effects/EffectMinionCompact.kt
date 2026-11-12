package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionCompactService
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.holder.get
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

/** Runs every compactor recipe a minion owns and currently meets the conditions for. */
class EffectMinionCompact(
    private val compacting: MinionCompactService
) : Effect<NoCompileData>("minion_compact") {
    override val description = "Compacts a minion's storage using the compactor recipes it owns."
    override val categories = setOf("minion", "inventory")
    override val additionalInfo = listOf(
        "Only works on minions: in a minion type's effects, an upgrade's effects, or level-up-effects. Anywhere else it does nothing.",
        "Recipes whose conditions fail are skipped. Does nothing if the minion owns no recipes."
    )

    override val parameters = setOf(TriggerParameter.LOCATION)

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: return false
        return compacting.compact(minion)
    }
}
