package com.exanthiax.ecominions.levelling.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.placeholders.LevelInjectable
import com.exanthiax.ecominions.levelling.model.LevelCurve
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.levelling.model.MinionCounter
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.evaluateExpression
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.executors.impl.NormalExecutorFactory
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.effects.arguments.EffectArguments
import com.willfp.libreforge.separatorAmbivalent
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.Triggers

/**
 * Parses a minion type's `levelling:` block.
 *
 * A broken levelling block degrades the type to non-levelling rather than
 * rejecting the file: the rest of the config is still valid and the minion
 * still mines. A levelling typo must not delete a working minion type.
 */
class LevellingLoader {

    fun load(config: Config, context: LoadContext): LevellingSpec {
        if (!config.has(SECTION)) {
            return LevellingSpec.DISABLED
        }

        val section = config.getSubsection(SECTION)
        val scoped = context.with(SECTION)

        if (!section.getBool("enabled")) {
            return LevellingSpec.DISABLED
        }

        val curve = readCurve(section, scoped)
        if (curve == null) {
            scoped.violation(
                "xp-formula",
                "levelling is enabled but no xp-formula or xp-requirements is set - " +
                    "the minion could never level, so levelling has been disabled for this type"
            )
            return LevellingSpec.DISABLED
        }

        return LevellingSpec(
            enabled = true,
            curve = curve,
            gainMethods = readGainMethods(section, scoped),
            conditions = Conditions.compile(
                section.getSubsections("conditions"),
                scoped.violationContext.with("conditions")
            ),
            levelUpEffects = Effects.compileChain(
                section.getSubsections("level-up-effects"),
                NormalExecutorFactory.create(),
                scoped.violationContext.with("level-up-effects")
            )
        )
    }

    private fun readCurve(section: Config, context: LoadContext): LevelCurve? {
        val maxLevel = section.getIntOrNull("max-level")
        val formula = section.getStringOrNull("xp-formula")
        val requirements = section.getDoublesOrNull("xp-requirements")

        if (formula != null && !requirements.isNullOrEmpty()) {
            context.violation(
                "xp-requirements",
                "both xp-formula and xp-requirements are set - xp-formula wins"
            )
        }

        if (formula != null) {
            return LevelCurve.Formula(formula, maxLevel) { expression, level ->
                evaluateExpression(
                    expression,
                    placeholderContext(injectable = LevelInjectable(level))
                )
            }
        }

        if (!requirements.isNullOrEmpty()) {
            return LevelCurve.Requirements(requirements, maxLevel)
        }

        return null
    }

    private fun readGainMethods(section: Config, context: LoadContext): List<MinionCounter> =
        section.getSubsections("xp-gain-methods").mapNotNull { raw ->
            readGainMethod(raw.separatorAmbivalent(), context.with("xp-gain-methods"))
        }

    private fun readGainMethod(config: Config, context: LoadContext): MinionCounter? {
        val triggerId = config.getStringOrNull("trigger")
            ?: return context.required(null, "trigger", "no trigger set - the method could never grant xp")

        val trigger = Triggers[triggerId]
            ?: return context.unknown("trigger", triggerId, Triggers.values().map { it.id })

        val multiplier = config.getStringOrNull("multiplier")
        val value = config.getStringOrNull("value")

        if (multiplier != null && value != null) {
            context.violation("value", "cannot specify both multiplier and value")
            return null
        }

        val violations = context.violationContext

        return MinionCounter(
            trigger = trigger,
            conditions = Conditions.compile(
                config.getSubsections("conditions"),
                violations.with("conditions")
            ),
            filters = Filters.compile(
                config.getSubsection("filters"),
                violations.with("filters")
            ),
            config = config.getSubsection("args"),
            arguments = EffectArguments.compile(
                config.getSubsection("args"),
                violations.with("args")
            ),
            multiplierExpression = multiplier,
            valueExpression = value
        ) { expression, dispatched -> evaluateFor(expression, dispatched) }
    }

    private fun evaluateFor(expression: String, dispatched: DispatchedTrigger): Double =
        evaluateExpression(
            expression,
            placeholderContext(player = dispatched.data.player)
        )

    private companion object {
        const val SECTION = "levelling"
    }
}
