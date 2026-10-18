package com.exanthiax.ecominions.libreforge.mutators

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.mutators.Mutator
import com.willfp.libreforge.triggers.TriggerData

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
