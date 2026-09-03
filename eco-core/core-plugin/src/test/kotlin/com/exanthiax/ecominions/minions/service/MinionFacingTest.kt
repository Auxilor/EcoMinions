package com.exanthiax.ecominions.minions.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionFacingTest {

    @Test
    fun `faces the placing player, squared to the nearest cardinal`() {
        assertEquals(180.0f, MinionFacing.towards(0.0f))
        assertEquals(270.0f, MinionFacing.towards(90.0f))
        assertEquals(0.0f, MinionFacing.towards(180.0f))
        assertEquals(90.0f, MinionFacing.towards(270.0f))
    }

    @Test
    fun `snaps to the nearest cardinal`() {
        assertEquals(180.0f, MinionFacing.towards(20.0f))
        assertEquals(270.0f, MinionFacing.towards(60.0f))
        assertEquals(180.0f, MinionFacing.towards(-40.0f))
    }

    @Test
    fun `normalises yaws outside a single turn`() {
        assertEquals(180.0f, MinionFacing.towards(720.0f))
        assertEquals(180.0f, MinionFacing.towards(-360.0f))
        assertEquals(270.0f, MinionFacing.towards(-270.0f))
    }
}
