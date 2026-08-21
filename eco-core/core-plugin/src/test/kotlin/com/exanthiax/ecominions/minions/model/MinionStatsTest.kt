package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionStatsTest {

    private val base = MinionType.BaseStats(
        speedTicks = "40",
        efficiencyTicks = "10",
        radius = "2",
        storageSlots = "1",
        maxStorageSlots = 54,
        regenDelayTicks = "60"
    )

    private fun stats(level: Int) = MinionStats(
        base = MinionType.BaseStats(
            speedTicks = "60 - %level%",
            efficiencyTicks = "10",
            radius = "3",
            storageSlots = "1",
            maxStorageSlots = 54,
            regenDelayTicks = "100"
        ) { expression, atLevel ->
            expression.replace("%level%", atLevel.toString())
                .split(" - ")
                .map { it.trim().toInt() }
                .reduce(Int::minus)
        },
        level = level
    )

    @Test
    fun `no contributions leaves base stats untouched`() {
        val stats = MinionStats(base, level = 1)

        assertEquals(40, stats.speedTicks)
        assertEquals(10, stats.efficiencyTicks)
        assertEquals(2, stats.radius)
        assertEquals(1, stats.storageSize)
        assertEquals(60, stats.regenDelayTicks)
    }

    @Test
    fun `base stats are evaluated at the minion's level`() {
        assertEquals(59, stats(level = 1).speedTicks)
        assertEquals(50, stats(level = 10).speedTicks)
    }

    @Test
    fun `changing level recomputes the base`() {
        val stats = stats(level = 1)
        stats.level = 20

        assertEquals(40, stats.speedTicks)
    }

    @Test
    fun `contributions layer on top of the level-scaled base`() {
        val stats = stats(level = 10)
        stats.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -5)

        assertEquals(45, stats.speedTicks)
    }

    @Test
    fun `clamping still applies after level scaling`() {
        val stats = stats(level = 10)
        stats.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -1000)

        assertEquals(MinionStat.SPEED_TICKS.min, stats.speedTicks)
    }

    @Test
    fun `contributions sum onto the base`() {
        val stats = MinionStats(base, level = 1)
        stats.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -15)
        stats.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -5)

        assertEquals(20, stats.speedTicks)
    }

    @Test
    fun `removing a contribution reverses it exactly`() {
        val stats = MinionStats(base, level = 1)
        val id = UUID.randomUUID()

        stats.contribute(MinionStat.RADIUS, id, 3)
        assertEquals(5, stats.radius)

        stats.remove(MinionStat.RADIUS, id)
        assertEquals(2, stats.radius)
    }

    @Test
    fun `re-contributing under the same id replaces rather than stacks`() {
        val stats = MinionStats(base, level = 1)
        val id = UUID.randomUUID()

        stats.contribute(MinionStat.RADIUS, id, 3)
        stats.contribute(MinionStat.RADIUS, id, 3)

        assertEquals(5, stats.radius)
    }

    @Test
    fun `speed clamps to one and efficiency clamps to zero`() {
        val stats = MinionStats(base, level = 1)
        stats.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -999)
        stats.contribute(MinionStat.EFFICIENCY_TICKS, UUID.randomUUID(), -999)

        assertEquals(1, stats.speedTicks)
        assertEquals(0, stats.efficiencyTicks)
    }

    @Test
    fun `inventory slots clamp to the type maximum`() {
        val stats = MinionStats(base, level = 1)
        stats.contribute(MinionStat.INVENTORY_SLOTS, UUID.randomUUID(), 999)

        assertEquals(54, stats.storageSize)
    }

    @Test
    fun `clamping happens on read so contribution order does not matter`() {
        val forwards = MinionStats(base, level = 1)
        forwards.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -100)
        forwards.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), 70)

        val backwards = MinionStats(base, level = 1)
        backwards.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), 70)
        backwards.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -100)

        assertEquals(10, forwards.speedTicks)
        assertEquals(forwards.speedTicks, backwards.speedTicks)
    }

    @Test
    fun `cycle ticks are speed plus efficiency, never below one`() {
        val stats = MinionStats(base, level = 1)

        assertEquals(50, stats.cycleTicks)
    }

    @Test
    fun `clearing removes every contribution`() {
        val stats = MinionStats(base, level = 1)
        stats.contribute(MinionStat.RADIUS, UUID.randomUUID(), 5)
        stats.contribute(MinionStat.SPEED_TICKS, UUID.randomUUID(), -20)

        stats.clearContributions()

        assertEquals(2, stats.radius)
        assertEquals(40, stats.speedTicks)
    }
}
