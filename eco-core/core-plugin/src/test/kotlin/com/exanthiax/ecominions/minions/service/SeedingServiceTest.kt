package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedingServiceTest {

    /** A minion on a solid floor seeds the nearest air block above it. */
    @Test
    fun `seeds nearest air block on a solid surface`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        assertTrue(service().tick(minion))
        assertEquals(1, world.countOf(Material.COAL_ORE))
    }

    @Test
    fun `does nothing when self-seeding is off`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = PlacementSettings.NONE)

        assertFalse(service().tick(minion))
        assertEquals(0, world.countOf(Material.COAL_ORE))
    }

    /** The minion's own column stays clear, so it never buries itself. */
    @Test
    fun `never seeds its own column`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        service().tick(minion)

        assertEquals(Material.AIR, world.typeAt(0, 0, 0))
        assertEquals(Material.AIR, world.typeAt(0, 1, 0))
    }

    /** Air over air is not a surface - a minion over a hole has nowhere to seed. */
    @Test
    fun `does not seed over a hole`() {
        val world = FakeWorld()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        assertFalse(service().tick(minion))
        assertEquals(0, world.countOf(Material.COAL_ORE))
    }

    /** A crop type declares its surface, and only that surface will do. */
    @Test
    fun `seeds a crop only on its configured surface`() {
        val world = FakeWorld()
        world.fillFloor(Material.STONE)
        val placement = PlacementSettings(
            liftRegen = true,
            surfaceBlock = Material.FARMLAND,
            selfSeed = true,
            seedBlock = Material.WHEAT
        )
        val minion = minionAt(world, placement = placement)

        assertFalse(service().tick(minion))

        world.set(1, -1, 0, Material.FARMLAND)
        assertTrue(service().tick(minion))
        assertEquals(Material.WHEAT, world.typeAt(1, 0, 0))
    }

    private fun service() = SeedingService()

    private fun selfSeeding(seed: Material) = PlacementSettings(
        liftRegen = false,
        surfaceBlock = null,
        selfSeed = true,
        seedBlock = seed
    )

    private fun minionAt(
        world: FakeWorld,
        placement: PlacementSettings,
        radius: Int = 1
    ): Minion {
        val type = MinionDefinitionFixture.build(placement = placement)
        return mockk<Minion>().also { minion ->
            every { minion.state } returns mockk { every { this@mockk.type } returns type }
            every { minion.stats } returns mockk { every { this@mockk.radius } returns radius }
            every { minion.entity } returns stand(world)
        }
    }

    private fun stand(world: FakeWorld): ArmorStand = world.stand()
}
