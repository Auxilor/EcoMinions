package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.levelling.model.LevelCurve
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.willfp.libreforge.conditions.ConditionList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MinionStateTest {

    private val type = MinionTypeFixture.build()

    private val levellingType = MinionTypeFixture.build(
        levelling = LevellingSpec(
            enabled = true,
            curve = LevelCurve.Requirements(listOf(50.0, 125.0), configuredMaxLevel = null),
            gainMethods = emptyList(),
            conditions = ConditionList(emptyList()),
            levelUpEffects = null
        )
    )

    @Test
    fun `a fresh minion is level one`() {
        assertEquals(1, MinionState(type = type).level)
    }

    @Test
    fun `level is one plus the sum of purchased tiers`() {
        val state = MinionState(type = type)
            .withUpgrade("speed", 3)
            .withUpgrade("capacity", 2)

        assertEquals(6, state.level)
    }

    @Test
    fun `buying a higher tier replaces the old one instead of stacking`() {
        val state = MinionState(type = type)
            .withUpgrade("speed", 1)
            .withUpgrade("speed", 4)

        assertEquals(4, state.upgradeLevel("speed"))
        assertEquals(5, state.level)
    }

    @Test
    fun `an unbought upgrade reads as tier zero`() {
        assertEquals(0, MinionState(type = type).upgradeLevel("fortune"))
    }

    @Test
    fun `states with equal contents are equal despite the byte array`() {
        val first = MinionState(type = type, inventoryBytes = byteArrayOf(1, 2, 3))
        val second = MinionState(type = type, inventoryBytes = byteArrayOf(1, 2, 3))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `buying an upgrade does not move the level of a levelling minion`() {
        val state = MinionState(type = levellingType, storedLevel = 2)
            .withUpgrade("speed", 3)

        assertEquals(2, state.level)
    }

    @Test
    fun `a levelling minion carries its xp`() {
        val state = MinionState(type = levellingType).withXp(xp = 30.0, level = 2)

        assertEquals(30.0, state.xp)
        assertEquals(2, state.level)
    }

    @Test
    fun `a level is clamped to the curve's maximum on read`() {
        val state = MinionState(type = levellingType, storedLevel = 99)

        assertEquals(3, state.level)
    }

    @Test
    fun `a non-levelling minion is never at max level`() {
        assertFalse(MinionState(type = type).isAtMaxLevel)
    }
}
