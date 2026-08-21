package com.exanthiax.ecominions.levelling.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevelCurveTest {

    @Test
    fun `the first requirement is the cost of reaching level two`() {
        val curve = LevelCurve.Requirements(listOf(50.0, 125.0, 200.0), configuredMaxLevel = null)

        assertEquals(50.0, curve.xpToReach(2))
        assertEquals(125.0, curve.xpToReach(3))
        assertEquals(200.0, curve.xpToReach(4))
    }

    @Test
    fun `a requirements list caps at one level above its size`() {
        val curve = LevelCurve.Requirements(listOf(50.0, 125.0, 200.0), configuredMaxLevel = null)

        assertEquals(4, curve.maxLevel)
    }

    @Test
    fun `an exhausted requirements list costs infinity`() {
        val curve = LevelCurve.Requirements(listOf(50.0), configuredMaxLevel = null)

        assertTrue(curve.xpToReach(3).isInfinite())
    }

    @Test
    fun `an explicit max level overrides the list length`() {
        val curve = LevelCurve.Requirements(listOf(50.0, 125.0), configuredMaxLevel = 2)

        assertEquals(2, curve.maxLevel)
    }

    @Test
    fun `a formula is evaluated with the level being reached`() {
        val seen = mutableListOf<Int>()
        val curve = LevelCurve.Formula(
            expression = "100 * %level%",
            configuredMaxLevel = 50
        ) { _, level ->
            seen += level
            100.0 * level
        }

        assertEquals(300.0, curve.xpToReach(3))
        assertEquals(listOf(3), seen)
    }

    @Test
    fun `a formula without a max level is unbounded`() {
        val curve = LevelCurve.Formula("100 * %level%", configuredMaxLevel = null) { _, level ->
            100.0 * level
        }

        assertEquals(Int.MAX_VALUE, curve.maxLevel)
    }
}
