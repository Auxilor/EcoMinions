package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.minions.MinionTypeFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionStateTest {

    private val type = MinionTypeFixture.build()

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
}
