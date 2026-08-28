package com.exanthiax.ecominions.libreforge.triggers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TriggerGroupMinionStaticTest {

    @Test
    fun `a plain integer creates a fixed-interval trigger`() {
        val trigger = TriggerGroupMinionStatic.create("200")

        assertEquals("minion_static_200", trigger?.id)
    }

    @Test
    fun `the same interval string returns the same trigger instance`() {
        val first = TriggerGroupMinionStatic.create("50")
        val second = TriggerGroupMinionStatic.create("50")

        assertTrue(first === second)
    }

    @Test
    fun `a non-integer value is treated as an expression, not rejected`() {
        val trigger = TriggerGroupMinionStatic.create("20 * 3")

        assertEquals("minion_static_20 * 3", trigger?.id)
    }

    @Test
    fun `a non-positive fixed interval is rejected`() {
        assertNull(TriggerGroupMinionStatic.create("0"))
        assertNull(TriggerGroupMinionStatic.create("-5"))
    }

    // No test for dueOn/dueDynamic's expression branch: evaluateExpressionOrNull needs a live
    // eco plugin instance to resolve, the same reason UpgradeLoader/CompactorService have no
    // unit tests either. Verified manually instead. dueFixed below never touches that path.

    // TriggerGroupMinionStatic is a singleton registry shared across tests in this JVM, so this
    // uses an interval no other test registers, and checks membership rather than an exact set
    // (dueFixed may also carry fixed-interval triggers other tests created).
    @Test
    fun `dueFixed only returns triggers whose interval divides the tick evenly`() {
        val hundred = TriggerGroupMinionStatic.create("101")
        val sixty = TriggerGroupMinionStatic.create("61")

        val dueAt6161 = TriggerGroupMinionStatic.dueFixed(6161).toSet()
        assertTrue(hundred in dueAt6161)
        assertTrue(sixty in dueAt6161)

        val dueAt6100 = TriggerGroupMinionStatic.dueFixed(6100).toSet()
        assertTrue(hundred !in dueAt6100)
        assertTrue(sixty in dueAt6100)

        val dueAtPrime = TriggerGroupMinionStatic.dueFixed(6163).toSet()
        assertTrue(hundred !in dueAtPrime)
        assertTrue(sixty !in dueAtPrime)
    }
}
