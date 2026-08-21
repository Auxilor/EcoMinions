package com.exanthiax.ecominions.levelling.model

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigurableElement
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.effects.arguments.EffectArgumentList
import com.willfp.libreforge.filters.FilterList
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.Trigger
import java.util.UUID

/**
 * One `xp-gain-method`, evaluated against a minion.
 *
 * Deliberately mirrors libreforge's own `Counter`/`BoundCounter` pair rather
 * than reusing it: `BoundCounter.accept` opens with
 * `trigger.dispatcher.get<Player>() ?: return`, and a minion dispatcher wraps a
 * Minion, so every minion dispatch would return immediately. `BoundCounters` is
 * internal, so the binding registry cannot be driven from here either.
 *
 * Evaluation order matches `BoundCounter` exactly, so admin config behaves the
 * same on a minion as it does on a player.
 */
class MinionCounter(
    val trigger: Trigger,
    private val conditions: ConditionList,
    private val filters: FilterList,
    override val config: Config,
    private val arguments: EffectArgumentList,
    private val multiplierExpression: String?,
    private val valueExpression: String?,
    private val evaluate: (String, DispatchedTrigger) -> Double
) : ConfigurableElement {

    override val uuid: UUID = UUID.randomUUID()

    /** @return The XP this method grants, or null if it does not apply at all. */
    fun amountFor(dispatched: DispatchedTrigger): Double? {
        if (dispatched.trigger != trigger) {
            return null
        }

        val data = dispatched.data

        if (!filters.isMet(data)) {
            return null
        }
        if (!conditions.areMetAndTrigger(dispatched)) {
            return null
        }

        for (filter in filters) {
            filter.config.addInjectablePlaceholder(dispatched.placeholders)
        }
        for (condition in conditions) {
            condition.config.addInjectablePlaceholder(dispatched.placeholders)
        }
        config.addInjectablePlaceholder(dispatched.placeholders)

        val (argumentsMet, met, notMet) = arguments.checkMet(this, dispatched)
        if (!argumentsMet) {
            notMet.forEach { it.ifNotMet(this, dispatched) }
            return null
        }
        met.forEach { it.ifMet(this, dispatched) }

        if (valueExpression != null) {
            return evaluate(valueExpression, dispatched)
        }
        if (multiplierExpression != null) {
            return data.value * evaluate(multiplierExpression, dispatched)
        }
        return data.value
    }
}
