package com.exanthiax.ecominions.api.minion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionTypeTest {

    @Test
    fun `parses a known type by its config spelling`() {
        assertEquals(MinionType.WOODCUTTER, MinionType.parse("woodcutter"))
    }

    @Test
    fun `parsing ignores case`() {
        assertEquals(MinionType.MINER, MinionType.parse("Miner"))
    }

    @Test
    fun `parsing ignores surrounding whitespace`() {
        assertEquals(MinionType.FARMER, MinionType.parse("  farmer  "))
    }

    @Test
    fun `returns null for an unknown type`() {
        assertNull(MinionType.parse("wizard"))
    }

    @Test
    fun `returns null for a missing type`() {
        assertNull(MinionType.parse(null))
    }

    @Test
    fun `returns null for a blank type`() {
        assertNull(MinionType.parse("   "))
    }

    @Test
    fun `every type has a lowercase config id`() {
        for (type in MinionType.entries) {
            assertEquals(type.name.lowercase(), type.id)
        }
    }

    @Test
    fun `collector parses`() {
        assertEquals(MinionType.COLLECTOR, MinionType.parse("collector"))
        assertEquals(MinionType.COLLECTOR, MinionType.parse("COLLECTOR"))
        assertEquals("collector", MinionType.COLLECTOR.id)
    }

    @Test
    fun `slayer parses`() {
        assertEquals(MinionType.SLAYER, MinionType.parse("slayer"))
        assertEquals("slayer", MinionType.SLAYER.id)
    }
}
