package com.exanthiax.ecominions.libreforge.mutators

import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.entity.Entity
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.mutator.Mutator
import wiki.ecohub.rules.trigger.TriggerData

/**
 * Retargets the chain at the minion in the trigger's victim.
 *
 * The reverse of [MutatorMinionOwnerAsDispatcher]: it lets a player's own effects
 * reach one specific minion, which a player owning several cannot otherwise name.
 */
class MutatorMinionAsDispatcher(
    private val minionAt: (Entity) -> Minion?
) : Mutator<NoCompileData>("minion_as_dispatcher") {
    override val description = "Makes the minion in the victim the dispatcher for the rest of the chain."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Use it with click_minion so a player's effects, like give_minion_xp from an item, reach the clicked minion.",
        "Does nothing when the victim is not a minion, so the rest of the chain still runs on the player."
    )

    override fun mutate(data: TriggerData, config: Config, compileData: NoCompileData): TriggerData {
        val minion = data.victim?.let(minionAt) ?: return data

        return data.copy(dispatcher = minion.toDispatcher())
    }
}
