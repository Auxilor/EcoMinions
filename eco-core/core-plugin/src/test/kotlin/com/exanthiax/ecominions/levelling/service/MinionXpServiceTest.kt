package com.exanthiax.ecominions.levelling.service

import com.exanthiax.ecominions.levelling.model.LevelCurve
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.willfp.libreforge.conditions.ConditionList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionXpServiceTest {

    private fun typeWith(curve: LevelCurve) = MinionDefinitionFixture.build(
        levelling = LevellingSpec(
            enabled = true,
            curve = curve,
            gainMethods = emptyList(),
            conditions = ConditionList(emptyList()),
            levelUpEffects = null
        )
    )

    private val laddered = typeWith(
        LevelCurve.Requirements(listOf(100.0, 200.0, 300.0), configuredMaxLevel = null)
    )

    @Test
    fun `xp below the requirement banks without levelling`() {
        val state = MinionState(type = laddered)

        val result = MinionXpService.progress(state, 40.0)

        assertEquals(1, result.newState.level)
        assertEquals(40.0, result.newState.xp)
        assertNull(result.levelsGained)
    }

    @Test
    fun `crossing a requirement levels up and keeps the remainder`() {
        val state = MinionState(type = laddered)

        val result = MinionXpService.progress(state, 130.0)

        assertEquals(2, result.newState.level)
        assertEquals(30.0, result.newState.xp)
        assertEquals(2..2, result.levelsGained)
    }

    @Test
    fun `one grant may cross several levels`() {
        val state = MinionState(type = laddered)

        val result = MinionXpService.progress(state, 100.0 + 200.0 + 10.0)

        assertEquals(3, result.newState.level)
        assertEquals(10.0, result.newState.xp)
        assertEquals(2..3, result.levelsGained)
    }

    @Test
    fun `xp stops accruing at max level`() {
        val state = MinionState(type = laddered, storedLevel = 4)

        val result = MinionXpService.progress(state, 5000.0)

        assertEquals(4, result.newState.level)
        assertEquals(0.0, result.newState.xp)
        assertNull(result.levelsGained)
    }

    @Test
    fun `a non-levelling minion banks nothing`() {
        val state = MinionState(type = MinionDefinitionFixture.build())

        val result = MinionXpService.progress(state, 500.0)

        assertEquals(0.0, result.newState.xp)
        assertNull(result.levelsGained)
    }

    @Test
    fun `a degenerate curve terminates instead of looping`() {
        val zeroed = typeWith(
            LevelCurve.Formula("0", configuredMaxLevel = 100) { _, _ -> 0.0 }
        )

        val result = MinionXpService.progress(MinionState(type = zeroed), 10.0)

        assertEquals(1, result.newState.level)
        assertNull(result.levelsGained)
    }
}
