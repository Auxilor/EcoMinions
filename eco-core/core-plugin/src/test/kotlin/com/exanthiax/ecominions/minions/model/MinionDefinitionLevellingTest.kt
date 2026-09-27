package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.willfp.eco.core.progression.LevelCurve
import com.willfp.libreforge.conditions.ConditionList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MinionDefinitionLevellingTest {

    @Test
    fun `a type without a levelling block cannot level`() {
        val type = MinionDefinitionFixture.build()

        assertFalse(type.levelling.enabled)
        assertEquals(1, type.levelling.maxLevel)
    }

    @Test
    fun `a levelling type reports the curve's max level`() {
        val type = MinionDefinitionFixture.build(
            levelling = LevellingSpec(
                enabled = true,
                curve = LevelCurve.Requirements(listOf(50.0, 125.0), startLevel = 1, configuredMaxLevel = null),
                gainMethods = emptyList(),
                conditions = ConditionList(emptyList()),
                levelUpEffects = null
            )
        )

        assertEquals(3, type.levelling.maxLevel)
    }
}
