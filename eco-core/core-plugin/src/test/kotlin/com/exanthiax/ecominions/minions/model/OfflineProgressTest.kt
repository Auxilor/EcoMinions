package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OfflineProgressTest {

    /** 50 ticks per cycle, so 2500ms of wall clock per cycle. */
    private val stats = MinionStats(
        speedTicks = 40,
        efficiencyTicks = 10,
        radius = 2,
        inventoryRows = 1,
        regenDelayTicks = 60,
        fortuneMultiplier = 1.0,
        autoSmelt = false,
        autoSell = false,
        xpDropAmount = 0,
        compactorEnabled = false
    )

    @Test
    fun `full efficiency credits one cycle per cycle length`() {
        assertEquals(4, OfflineProgress.cycles(elapsedMillis = 10_000, efficiency = 1.0, stats = stats))
    }

    @Test
    fun `reduced efficiency credits proportionally fewer cycles`() {
        assertEquals(2, OfflineProgress.cycles(elapsedMillis = 10_000, efficiency = 0.5, stats = stats))
    }

    @Test
    fun `partial cycles are not credited`() {
        assertEquals(0, OfflineProgress.cycles(elapsedMillis = 2_499, efficiency = 1.0, stats = stats))
    }

    @Test
    fun `a clock that went backwards credits nothing`() {
        assertEquals(0, OfflineProgress.cycles(elapsedMillis = -5_000, efficiency = 1.0, stats = stats))
    }

    @Test
    fun `disabling efficiency credits nothing`() {
        assertEquals(0, OfflineProgress.cycles(elapsedMillis = 10_000, efficiency = 0.0, stats = stats))
    }

    @Test
    fun `an implausibly long absence does not overflow`() {
        val cycles = OfflineProgress.cycles(elapsedMillis = Long.MAX_VALUE / 2, efficiency = 1.0, stats = stats)

        assertEquals(Int.MAX_VALUE, cycles)
    }
}
