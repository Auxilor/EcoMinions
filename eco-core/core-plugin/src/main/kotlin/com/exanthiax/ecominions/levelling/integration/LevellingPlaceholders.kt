package com.exanthiax.ecominions.levelling.integration

import com.exanthiax.ecominions.core.placeholders.DynamicInternalPlaceholder
import com.exanthiax.ecominions.core.placeholders.InternalPlaceholder
import com.exanthiax.ecominions.core.placeholders.PlaceholderSource
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.progression.ProgressionPlaceholders
import com.willfp.eco.util.toNumeral

/**
 * The levelling slice's internal placeholders.
 *
 * Resolves against the same context key the minion placeholders use, so these
 * work anywhere `%level%` already does - hologram lines, the main menu title,
 * and spawn-item lore.
 */
class LevellingPlaceholders(
    private val infinityText: () -> String
) : PlaceholderSource {

    override fun internalPlaceholders(): List<InternalPlaceholder> = listOf(
        InternalPlaceholder("xp") { it.minion()?.state?.xp?.toInt()?.toString() },
        InternalPlaceholder("xp_required") { context ->
            val minion = context.minion() ?: return@InternalPlaceholder null
            requiredFor(minion)?.toInt()?.toString() ?: infinityText()
        },
        InternalPlaceholder("progress") { context ->
            val minion = context.minion() ?: return@InternalPlaceholder null
            val required = requiredFor(minion) ?: return@InternalPlaceholder "100"
            ((minion.state.xp / required) * 100).toInt().coerceIn(0, 100).toString()
        },
        InternalPlaceholder("max_level") { it.minion()?.state?.definition?.levelling?.maxLevel?.toString() },
        InternalPlaceholder("level_numeral") { it.minion()?.level?.toNumeral() },
        InternalPlaceholder("previous_level") { it.minion()?.let { m -> (m.level - 1).toString() } },
        InternalPlaceholder("previous_level_numeral") { it.minion()?.let { m -> (m.level - 1).toNumeral() } }
    )

    /**
     * `%level_2%`, `%level_-1_numeral%` and so on, relative to the minion's current level.
     *
     * Every other levelling plugin already offered this in its lore; minions did not, which
     * made them the odd one out for no reason other than nobody having copied the regex here.
     * It now comes from the same shared resolver in eco that the others use.
     */
    override fun dynamicPlaceholders(): List<DynamicInternalPlaceholder> = listOf(
        DynamicInternalPlaceholder { key, context ->
            val minion = context.minion() ?: return@DynamicInternalPlaceholder null
            ProgressionPlaceholders.resolveOffset(key, "level", minion.level)
        }
    )

    /** @return XP needed for the next level, or null when there is no next level. */
    private fun requiredFor(minion: Minion): Double? {
        val levelling = minion.state.definition.levelling
        if (!levelling.enabled || minion.state.isAtMaxLevel) {
            return null
        }
        val required = levelling.curve?.xpToReach(minion.state.level + 1) ?: return null
        return required.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun TemplateContext.minion(): Minion? = get(MinionPlaceholders.MINION)
}
