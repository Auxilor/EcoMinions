package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionStatsCapacityTest {

    private val base = MinionType.BaseStats(
        speedTicks = 40,
        efficiencyTicks = 10,
        radius = 2,
        inventoryRows = 3,
        regenDelayTicks = 60
    )

    @Test
    fun `storage size tracks the clamped row count`() {
        val stats = MinionStats(base)
        assertEquals(27, stats.storageSize)

        val id = UUID.randomUUID()
        stats.contribute(MinionStat.INVENTORY_ROWS, id, 2)
        assertEquals(45, stats.storageSize)

        stats.remove(MinionStat.INVENTORY_ROWS, id)
        assertEquals(27, stats.storageSize)
    }

    @Test
    fun `rows never exceed six however many effects contribute`() {
        val stats = MinionStats(base)
        repeat(5) { stats.contribute(MinionStat.INVENTORY_ROWS, UUID.randomUUID(), 2) }

        assertEquals(6, stats.inventoryRows)
        assertEquals(54, stats.storageSize)
    }
}
