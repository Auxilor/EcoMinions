package com.exanthiax.ecominions.libreforge.mutators

import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.mutator.Mutator
import wiki.ecohub.rules.trigger.TriggerData

/** Moves the chain's location onto the block the minion is acting on. */
object MutatorMinionLocationToTargetBlock : Mutator<NoCompileData>("minion_location_to_target_block") {
    override val description = "Sets the location to the block the minion is acting on."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works in a chain run by a minion."
    )

    override fun mutate(data: TriggerData, config: Config, compileData: NoCompileData): TriggerData {
        val block = data.block ?: return data
        return data.copy(location = block.location)
    }
}
