package com.exanthiax.ecominions.levelling.integration

import com.exanthiax.ecominions.core.placeholders.InternalPlaceholder
import com.exanthiax.ecominions.core.placeholders.PlaceholderSource
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Numerals
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.service.Minion

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
        InternalPlaceholder("max_level") { it.minion()?.state?.type?.levelling?.maxLevel?.toString() },
        InternalPlaceholder("level_numeral") { it.minion()?.level?.let(Numerals::roman) }
    )

    /** @return XP needed for the next level, or null when there is no next level. */
    private fun requiredFor(minion: Minion): Double? {
        val levelling = minion.state.type.levelling
        if (!levelling.enabled || minion.state.isAtMaxLevel) {
            return null
        }
        val required = levelling.curve?.xpToReach(minion.state.level + 1) ?: return null
        return required.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun TemplateContext.minion(): Minion? = get(MinionPlaceholders.MINION)
}
