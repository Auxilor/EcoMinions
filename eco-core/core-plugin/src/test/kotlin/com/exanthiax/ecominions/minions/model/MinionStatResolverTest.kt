package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionStatResolverTest {

    private val base = MinionType.BaseStats(
        speedTicks = 40,
        efficiencyTicks = 10,
        radius = 2,
        inventoryRows = 1,
        regenDelayTicks = 60
    )

    @Test
    fun `no upgrades leaves base stats untouched`() {
        val stats = MinionStatResolver.resolve(base, emptyList())

        assertEquals(40, stats.speedTicks)
        assertEquals(10, stats.efficiencyTicks)
        assertEquals(2, stats.radius)
        assertEquals(1, stats.inventoryRows)
        assertEquals(1.0, stats.fortuneMultiplier)
        assertFalse(stats.autoSell)
    }

    @Test
    fun `speed and efficiency subtract ticks`() {
        val stats = MinionStatResolver.resolve(
            base,
            listOf(UpgradeEffect("speed", 15.0), UpgradeEffect("efficiency", 4.0))
        )

        assertEquals(25, stats.speedTicks)
        assertEquals(6, stats.efficiencyTicks)
    }

    @Test
    fun `speed never drops below one tick`() {
        val stats = MinionStatResolver.resolve(base, listOf(UpgradeEffect("speed", 999.0)))

        assertEquals(1, stats.speedTicks)
    }

    @Test
    fun `efficiency floors at zero rather than going negative`() {
        val stats = MinionStatResolver.resolve(base, listOf(UpgradeEffect("efficiency", 999.0)))

        assertEquals(0, stats.efficiencyTicks)
    }

    @Test
    fun `fortune tiers compound rather than adding`() {
        val stats = MinionStatResolver.resolve(
            base,
            listOf(UpgradeEffect("fortune", 0.5), UpgradeEffect("fortune", 0.5))
        )

        assertEquals(2.25, stats.fortuneMultiplier)
    }

    @Test
    fun `capacity is clamped to a double chest`() {
        val stats = MinionStatResolver.resolve(base, listOf(UpgradeEffect("capacity", 99.0)))

        assertEquals(6, stats.inventoryRows)
        assertEquals(54, stats.storageSize)
    }

    @Test
    fun `zero valued toggles stay off`() {
        val stats = MinionStatResolver.resolve(
            base,
            listOf(UpgradeEffect("auto_sell", 0.0), UpgradeEffect("auto_smelt", 1.0))
        )

        assertFalse(stats.autoSell)
        assertTrue(stats.autoSmelt)
    }

    @Test
    fun `unknown effects are ignored instead of throwing`() {
        val stats = MinionStatResolver.resolve(base, listOf(UpgradeEffect("teleportation", 5.0)))

        assertEquals(40, stats.speedTicks)
    }

    @Test
    fun `regen delay is not upgradable`() {
        val stats = MinionStatResolver.resolve(base, listOf(UpgradeEffect("speed", 5.0)))

        assertEquals(60, stats.regenDelayTicks)
    }

    @Test
    fun `cycle time is speed plus efficiency`() {
        val stats = MinionStatResolver.resolve(base, emptyList())

        assertEquals(50, stats.cycleTicks)
    }
}
