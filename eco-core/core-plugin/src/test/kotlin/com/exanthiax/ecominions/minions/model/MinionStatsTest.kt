package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionStatsTest {

    private fun stats(level: Int) = MinionStats(
        base = MinionType.BaseStats(
            speedTicks = "60 - %level%",
            efficiencyTicks = "10",
            radius = "3",
            inventoryRows = "1",
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
}
