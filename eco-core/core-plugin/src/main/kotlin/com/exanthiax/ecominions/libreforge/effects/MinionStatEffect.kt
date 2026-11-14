package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.exanthiax.ecominions.minions.service.Minion
import wiki.ecohub.config.Config
import wiki.ecohub.config.section.getIntFromExpression
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.effect.Identifiers
import wiki.ecohub.rules.effect.RunOrder
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get
import wiki.ecohub.rules.trigger.TriggerData

/**
 * A permanent effect that moves one minion stat while its holder is active.
 *
 * The contribution is keyed by libreforge's own identifier, so enabling and
 * disabling are exact inverses even when the same upgrade appears twice. Clamping
 * is deliberately *not* done here - it belongs to the stat and happens on read,
 * or the result would depend on which holder libreforge walked first.
 */
abstract class MinionStatEffect(
    id: String,
    private val stat: MinionStat,
    private val argument: String,
    /** -1 for effects whose config value reads as "faster"/"less". */
    private val sign: Int = 1
) : Effect<NoCompileData>(id) {

    override val runOrder = RunOrder.START

    override fun onEnable(
        dispatcher: Dispatcher<*>,
        config: Config,
        identifiers: Identifiers,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ) {
        val minion = dispatcher.get<Minion>() ?: return
        val amount = config.getIntFromExpression(argument, null as TriggerData?) * sign

        minion.stats.contribute(stat, identifiers.uuid, amount)
        afterChange(minion)
    }

    override fun onDisable(
        dispatcher: Dispatcher<*>,
        identifiers: Identifiers,
        holder: ProvidedHolder
    ) {
        val minion = dispatcher.get<Minion>() ?: return

        minion.stats.remove(stat, identifiers.uuid)
        afterChange(minion)
    }

    /** Hook for stats that need more than a number changing. */
    protected open fun afterChange(minion: Minion) {
        // Override when needed.
    }
}
