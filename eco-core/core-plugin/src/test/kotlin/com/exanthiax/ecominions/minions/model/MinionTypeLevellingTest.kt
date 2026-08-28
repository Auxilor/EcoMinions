package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.levelling.model.LevelCurve
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.willfp.libreforge.conditions.ConditionList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MinionTypeLevellingTest {

    @Test
    fun `a type without a levelling block cannot level`() {
        val type = MinionTypeFixture.build()

        assertFalse(type.levelling.enabled)
        assertEquals(1, type.levelling.maxLevel)
    }

    @Test
    fun `a levelling type reports the curve's max level`() {
        val type = MinionTypeFixture.build(
            levelling = LevellingSpec(
                enabled = true,
                curve = LevelCurve.Requirements(listOf(50.0, 125.0), configuredMaxLevel = null),
                gainMethods = emptyList(),
                conditions = ConditionList(emptyList()),
                levelUpEffects = null
            )
        )

        assertEquals(3, type.levelling.maxLevel)
    }
}
