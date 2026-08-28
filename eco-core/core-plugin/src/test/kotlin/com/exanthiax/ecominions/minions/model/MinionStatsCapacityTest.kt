package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionStatsCapacityTest {

    private val base = MinionType.BaseStats(
        speedTicks = "40",
        efficiencyTicks = "10",
        radius = "2",
        storageSlots = "27",
        maxStorageSlots = "54",
        hardSlotCap = 54,
        regenDelayTicks = "60"
    )

    @Test
    fun `storage size tracks contributed slots`() {
        val stats = MinionStats(base, level = 1)
        assertEquals(27, stats.storageSize)

        val id = UUID.randomUUID()
        stats.contribute(MinionStat.INVENTORY_SLOTS, id, 9)
        assertEquals(36, stats.storageSize)

        stats.remove(MinionStat.INVENTORY_SLOTS, id)
        assertEquals(27, stats.storageSize)
    }

    @Test
    fun `slots never exceed the type's own maximum`() {
        val stats = MinionStats(base, level = 1)
        repeat(5) { stats.contribute(MinionStat.INVENTORY_SLOTS, UUID.randomUUID(), 20) }

        assertEquals(54, stats.storageSize)
    }

    @Test
    fun `a type may cap higher than a full double chest`() {
        val stats = MinionStats(base.copy(storageSlots = "100", maxStorageSlots = "512", hardSlotCap = 512), level = 1)
        stats.contribute(MinionStat.INVENTORY_SLOTS, UUID.randomUUID(), 300)

        assertEquals(400, stats.storageSize)
    }

    @Test
    fun `slots never fall below one`() {
        val stats = MinionStats(base, level = 1)
        stats.contribute(MinionStat.INVENTORY_SLOTS, UUID.randomUUID(), -999)

        assertEquals(1, stats.storageSize)
    }
}
