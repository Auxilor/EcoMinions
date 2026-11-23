package com.exanthiax.ecominions.levelling.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.placeholders.LevelInjectable
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.levelling.model.MinionCounter
import wiki.ecohub.config.Config
import wiki.ecohub.config.section.separatorAmbivalent
import wiki.ecohub.math.evaluateExpression
import wiki.ecohub.modules.progression.LevelCurve
import wiki.ecohub.modules.progression.LevelCurves
import wiki.ecohub.rules.argument.EffectArguments
import wiki.ecohub.rules.chain.NormalExecutorFactory
import wiki.ecohub.rules.condition.Conditions
import wiki.ecohub.rules.effect.Effects
import wiki.ecohub.rules.filter.Filters
import wiki.ecohub.rules.trigger.DispatchedTrigger
import wiki.ecohub.rules.trigger.Triggers
import wiki.ecohub.ui.placeholder.placeholderContext

/**
 * Parses a minion definition's `levelling:` block.
 *
 * A broken levelling block degrades the type to non-levelling rather than
 * rejecting the file: the rest of the config is still valid and the minion
 * still mines. A levelling typo must not delete a working minion.
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

    internal fun readCurve(section: Config, context: LoadContext): LevelCurve? {
        val maxLevel = section.getIntOrNull("max-level")
        val formula = section.getStringOrNull("xp-formula")
        val requirements = section.getDoublesOrNull("xp-requirements")

        val parsed = LevelCurves.parse(
            formula = formula,
            requirements = requirements,
            maxLevel = maxLevel,
            startLevel = 1,
            freeFirstLevel = false
        ) { expression, level ->
            evaluateExpression(
                expression,
                placeholderContext(injectable = LevelInjectable(level))
            )
        }

        if (parsed.curve is LevelCurve.None) {
            // load() reports this case itself, with a message specific to a
            // levelling block that is enabled but has nothing to level from -
            // skip LevelCurves' generic problem so the existing violation
            // output is not duplicated.
            return null
        }

        for (problem in parsed.problems) {
            context.violation(problem.path, problem.message)
        }

        return parsed.curve
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
