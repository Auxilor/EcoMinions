package com.exanthiax.ecominions.levelling.model

import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.effects.Chain

/**
 * A minion definition's whole `levelling:` block.
 *
 * When [enabled] is false the type keeps the original behaviour, where level is
 * the sum of purchased upgrade tiers plus one, and [curve] is null.
 */
class LevellingSpec(
    val enabled: Boolean,
    val curve: LevelCurve?,
    val gainMethods: List<MinionCounter>,
    val conditions: ConditionList,
    val levelUpEffects: Chain?
) {
    /** The highest level a minion of this type can reach, or 1 when it cannot level. */
    val maxLevel: Int
        get() = if (enabled) curve?.maxLevel ?: 1 else 1

    companion object {
        val DISABLED = LevellingSpec(
            enabled = false,
            curve = null,
            gainMethods = emptyList(),
            conditions = ConditionList(emptyList()),
            levelUpEffects = null
        )
    }
}
