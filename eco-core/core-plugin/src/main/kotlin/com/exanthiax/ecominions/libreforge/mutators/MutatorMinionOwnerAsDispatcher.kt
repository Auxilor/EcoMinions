package com.exanthiax.ecominions.libreforge.mutators

import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.get
import com.willfp.libreforge.mutators.Mutator
import com.willfp.libreforge.mutators.parameterTransformers
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

/**
 * Retargets the chain at the minion's owner.
 *
 * Exists so an admin can say "run the rest of this on the player" explicitly,
 * rather than relying on the fact that player-targeting effects happen to find a
 * player in the trigger data.
 */
object MutatorMinionOwnerAsDispatcher : Mutator<NoCompileData>("minion_owner_as_dispatcher") {
    override val description = "Makes the minion's owner the dispatcher for the rest of the chain."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works in a chain run by a minion.",
        "Does nothing while the owner is offline, so the rest of the chain still runs on the minion."
    )

    override val parameterTransformers = parameterTransformers {
        adds(TriggerParameter.PLAYER)
    }

    override fun mutate(data: TriggerData, config: Config, compileData: NoCompileData): TriggerData {
        val minion = data.dispatcher.get<Minion>() ?: return data
        val owner = minion.ownerPlayer() ?: return data

        return data.copy(dispatcher = owner.toDispatcher(), player = owner)
    }
}
