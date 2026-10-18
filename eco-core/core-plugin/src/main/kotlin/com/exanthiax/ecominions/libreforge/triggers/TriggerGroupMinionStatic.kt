package com.exanthiax.ecominions.libreforge.triggers

import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.evaluateExpressionOrNull
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerGroup
import com.willfp.libreforge.triggers.TriggerParameter
import java.util.concurrent.ConcurrentHashMap

/**
 * `minion_static_<n>` - fires once every n ticks, only for minions that are due,
 * instead of dispatching every tick and relying on `every:` to discard most of
 * it at the effect layer. Mirrors libreforge's own `static` trigger group,
 * minion-scoped rather than player-scoped.
 *
 * A plain integer (`minion_static_200`) is the fast path: just a modulo check.
 * Anything else (`minion_static_%some_placeholder%`, an arithmetic expression)
 * is re-evaluated every time [dueOn] runs, same tradeoff as libreforge's own
 * dynamic `static`/`global_static` triggers.
 */
object TriggerGroupMinionStatic : TriggerGroup("minion_static") {

    private val fixed = ConcurrentHashMap<Int, MinionStaticTrigger>()
    private val dynamic = ConcurrentHashMap<String, MinionDynamicStaticTrigger>()

    override fun create(value: String): Trigger? {
        val interval = value.toIntOrNull()
        if (interval != null) {
            if (interval < 1) {
                return null
            }
            return fixed.getOrPut(interval) { MinionStaticTrigger(interval) }
        }
        return dynamic.getOrPut(value) { MinionDynamicStaticTrigger(value) }
    }

    /** Every registered `minion_static_*` trigger whose interval divides [tick] evenly. */
    fun dueOn(tick: Long): List<Trigger> = dueFixed(tick) + dueDynamic(tick)

    /** Exposed for testing: the modulo check alone, no expression evaluation. */
    internal fun dueFixed(tick: Long): List<Trigger> = fixed.values.filter { tick % it.interval == 0L }

    private fun dueDynamic(tick: Long): List<Trigger> = dynamic.values.filter { trigger ->
        val interval = evaluateExpressionOrNull(trigger.expression, placeholderContext())?.toInt()
        interval != null && interval >= 1 && tick % interval == 0L
    }

    class MinionStaticTrigger(val interval: Int) : Trigger("minion_static_$interval") {
        override val description = "Fires for each working minion on a fixed tick interval."
        override val categories = setOf("minion")
        override val additionalInfo = listOf(
            "Only fires for minions. Use it in a minion type's effects or an upgrade's effects.",
            "Use minion_static_<ticks>, e.g. minion_static_200 for every 200 ticks.",
            "Carries no player, so effects that need one cannot use it."
        )
        override val parameterDescriptions = mapOf(
            TriggerParameter.LOCATION to "The minion's location."
        )
        override val parameters = setOf(TriggerParameter.LOCATION)
    }

    class MinionDynamicStaticTrigger(val expression: String) : Trigger("minion_static_$expression") {
        override val description = "Fires for each working minion on a tick interval set by an expression."
        override val categories = setOf("minion")
        override val additionalInfo = listOf(
            "Only fires for minions. Use it in a minion type's effects or an upgrade's effects.",
            "The expression is worked out again on every check, so it can use placeholders."
        )
        override val parameterDescriptions = mapOf(
            TriggerParameter.LOCATION to "The minion's location."
        )
        override val parameters = setOf(TriggerParameter.LOCATION)
    }
}
