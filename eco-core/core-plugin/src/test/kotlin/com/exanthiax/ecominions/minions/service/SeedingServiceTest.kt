package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedingServiceTest {

    /** One block per pass, the nearest one. */
    @Test
    fun `seeds the nearest air block on a solid surface`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        assertTrue(service().tick(minion, now = 0L))
        assertEquals(1, world.countOf(Material.COAL_ORE))
    }

    /** The area fills a block at a time, one regen delay apart. */
    @Test
    fun `seeds again only once the regen delay has passed`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))
        val service = service()

        assertTrue(service.tick(minion, now = 0L))
        assertFalse(service.tick(minion, now = REGEN_DELAY_TICKS - 1L))
        assertEquals(1, world.countOf(Material.COAL_ORE))

        assertTrue(service.tick(minion, now = REGEN_DELAY_TICKS.toLong()))
        assertEquals(2, world.countOf(Material.COAL_ORE))
    }

    /** A full area has nothing pending, so the minion reports blocked rather than waiting. */
    @Test
    fun `is pending only while somewhere is left to seed`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 2)

        assertTrue(service().isPending(minion))

        world.fillLayer(0, Material.COAL_ORE)
        assertFalse(service().isPending(minion))
    }

    @Test
    fun `does nothing when self-seeding is off`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = PlacementSettings.NONE)

        assertFalse(service().tick(minion, now = 0L))
        assertEquals(0, world.countOf(Material.COAL_ORE))
    }

    /** The minion's own column stays clear, so it never buries itself. */
    @Test
    fun `never seeds its own column`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        service().tick(minion, now = 0L)

        assertEquals(Material.AIR, world.typeAt(0, 0, 0))
        assertEquals(Material.AIR, world.typeAt(0, 1, 0))
    }

    /** Air over air is not a surface - a minion over a hole has nowhere to seed. */
    @Test
    fun `does not seed over a hole`() {
        val world = FakeWorld()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        assertFalse(service().tick(minion, now = 0L))
        assertEquals(0, world.countOf(Material.COAL_ORE))
    }

    /** A crop type declares its surface, and only that surface will do. */
    @Test
    fun `seeds a crop only on its configured surface`() {
        val world = FakeWorld()
        world.fillFloor(Material.STONE)
        val placement = PlacementSettings(
            liftRegen = true,
            surfaceBlock = FakeBlockLookup.testable(Material.FARMLAND),
            selfSeed = true,
            seedBlock = FakeBlockLookup.testable(Material.WHEAT)
        )
        val minion = minionAt(world, placement = placement)

        assertFalse(service().tick(minion, now = 0L))

        world.set(1, -1, 0, Material.FARMLAND)
        assertTrue(service().tick(minion, now = 0L))
        assertEquals(Material.WHEAT, world.typeAt(1, 0, 0))
        assertEquals(1, world.countOf(Material.WHEAT))
    }

    /**
     * A seed above the mining box would never be mined again, so the minion
     * would bury itself in ore it cannot reach.
     */
    @Test
    fun `does not seed above the mining box`() {
        val world = FakeWorld()
        world.fillFloor()
        world.fillLayer(0)
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 2)

        assertFalse(service().tick(minion, now = 0L))
        assertEquals(0, world.countOf(Material.COAL_ORE))
    }

    /** The same spots are fair game once the box is tall enough to mine them back. */
    @Test
    fun `seeds higher up when the height reaches there`() {
        val world = FakeWorld()
        world.fillFloor()
        world.fillLayer(0)
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 3)

        assertTrue(service().tick(minion, now = 0L))
        assertEquals(Material.COAL_ORE, world.typeAt(-1, 1, 0))
    }

    private fun service() = SeedingService()

    private fun selfSeeding(seed: Material) = PlacementSettings(
        liftRegen = false,
        surfaceBlock = null,
        selfSeed = true,
        seedBlock = FakeBlockLookup.testable(seed)
    )

    private fun minionAt(
        world: FakeWorld,
        placement: PlacementSettings,
        radius: Int = 1,
        height: Int = 3
    ): Minion {
        val type = MinionDefinitionFixture.build(placement = placement)
        return mockk<Minion>().also { minion ->
            every { minion.state } returns mockk { every { this@mockk.definition } returns type }
            every { minion.stats } returns mockk {
                every { this@mockk.radius } returns radius
                every { this@mockk.height } returns height
                every { this@mockk.regenDelayTicks } returns REGEN_DELAY_TICKS
            }
            every { minion.entity } returns stand(world)
            every { minion.mining } returns MiningState()
        }
    }

    private fun stand(world: FakeWorld): ArmorStand = world.stand()

    private companion object {
        const val REGEN_DELAY_TICKS = 60
    }
}
