package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OfflineProgressTest {

    /** 50 ticks per cycle, so 2500ms of wall clock per unfuelled cycle. */
    private val stats = MinionStats(
        MinionType.BaseStats(
            speedTicks = "40",
            efficiencyTicks = "10",
            radius = "2",
            storageSlots = "1",
            maxStorageSlots = "54",
            hardSlotCap = 54,
            regenDelayTicks = "60"
        ),
        level = 1
    )

    /** A doubled-speed fuel: 20 speed ticks + 10 efficiency = 30, so 1500ms per cycle. */
    private val fueledCycleTicks = 30

    private fun simulate(
        elapsedMillis: Long,
        efficiency: Double = 1.0,
        fuelRemainingMillis: Long = 0L,
        stopAfter: Int = Int.MAX_VALUE
    ): OfflineProgress.Result {
        var run = 0
        return OfflineProgress.simulate(
            elapsedMillis = elapsedMillis,
            efficiency = efficiency,
            stats = stats,
            fuelRemainingMillis = fuelRemainingMillis,
            fueledCycleTicks = fueledCycleTicks
        ) {
            run++
            run <= stopAfter
        }
    }

    @Test
    fun `unfuelled full efficiency credits one cycle per cycle length`() {
        assertEquals(4, simulate(elapsedMillis = 10_000).cycles)
    }

    @Test
    fun `reduced efficiency credits proportionally fewer cycles`() {
        assertEquals(2, simulate(elapsedMillis = 10_000, efficiency = 0.5).cycles)
    }

    @Test
    fun `partial cycles are not credited`() {
        assertEquals(0, simulate(elapsedMillis = 2_499).cycles)
    }

    @Test
    fun `a clock that went backwards credits nothing`() {
        assertEquals(0, simulate(elapsedMillis = -5_000).cycles)
    }

    @Test
    fun `disabling efficiency credits nothing`() {
        assertEquals(0, simulate(elapsedMillis = 10_000, efficiency = 0.0).cycles)
    }

    @Test
    fun `fuel covering the whole window runs every cycle at the fuelled rate`() {
        // 6000ms of window, 1500ms per fuelled cycle, 6000ms of fuel.
        val result = simulate(elapsedMillis = 6_000, fuelRemainingMillis = 6_000)

        assertEquals(4, result.cycles)
        assertEquals(6_000L, result.fuelMillisConsumed)
    }

    @Test
    fun `fuel running out mid window falls back to the base rate`() {
        // 3000ms of fuel buys two 1500ms cycles, then 4000ms of window remains
        // at 2500ms per cycle, buying one more with 1500ms left over.
        val result = simulate(elapsedMillis = 7_000, fuelRemainingMillis = 3_000)

        assertEquals(3, result.cycles)
        assertEquals(3_000L, result.fuelMillisConsumed)
    }

    @Test
    fun `fuel outlasting the window is only partly consumed`() {
        val result = simulate(elapsedMillis = 3_000, fuelRemainingMillis = 60_000)

        assertEquals(2, result.cycles)
        assertEquals(3_000L, result.fuelMillisConsumed)
    }

    @Test
    fun `stopping early leaves the remaining fuel unspent`() {
        // Storage fills after one cycle; only that cycle's fuel is consumed.
        val result = simulate(elapsedMillis = 60_000, fuelRemainingMillis = 60_000, stopAfter = 1)

        assertEquals(1, result.cycles)
        assertEquals(1_500L, result.fuelMillisConsumed)
    }

    @Test
    fun `an implausibly long absence is capped rather than walked forever`() {
        val result = OfflineProgress.simulate(
            elapsedMillis = Long.MAX_VALUE / 2,
            efficiency = 1.0,
            stats = stats,
            fuelRemainingMillis = 0L,
            fueledCycleTicks = fueledCycleTicks
        ) { true }

        assertEquals(OfflineProgress.MAX_SIMULATED_CYCLES, result.cycles)
    }
}
