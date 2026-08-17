package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.Identifiers
import com.willfp.libreforge.effects.RunOrder
import com.willfp.libreforge.get
import com.willfp.libreforge.getIntFromExpression
import com.willfp.libreforge.triggers.TriggerData

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

    override val arguments = arguments {
        require(
            argument,
            "You must specify $argument!",
            description = "How far to move the stat. Supports expressions.",
            type = ArgType.EXPRESSION
        )
    }

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
