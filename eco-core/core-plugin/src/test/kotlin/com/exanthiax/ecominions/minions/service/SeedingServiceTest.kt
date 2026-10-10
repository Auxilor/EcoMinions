package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.willfp.eco.core.blocks.TestableBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.BlockData
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class SeedingServiceTest {

    /** One block per pass, the nearest one. */
    @Test
    fun `seeds the nearest air block on a solid surface`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE))

        assertTrue(service().tick(minion))
        assertEquals(1, world.countOf(Material.COAL_ORE))
    }

    /** The whole area is seeded before anything is mined, a block per cycle. */
    @Test
    fun `keeps seeding until the area is full`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 2)
        val service = service()

        repeat(SPOTS_AROUND_MINION) {
            assertTrue(service.tick(minion))
        }

        assertEquals(SPOTS_AROUND_MINION, world.countOf(Material.COAL_ORE))
        assertFalse(service.tick(minion))
    }

    /**
     * A block that was mined is on its way back on the regen timer. Seeding it
     * again would hand the minion a fresh target every cycle and let it mine
     * with no regen delay at all.
     */
    @Test
    fun `leaves a spot that is waiting on regen`() {
        val world = FakeWorld()
        world.fillFloor()
        world.fillLayer(0, Material.COAL_ORE)
        world.set(1, 0, 0, Material.AIR)
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 2)
        minion.mining.regenQueue += MiningState.RegenEntry(
            location = world.blockAt(1, 0, 0).location,
            regenData = mockk<BlockData>(),
            surfaceBlock = null,
            regenAt = REGEN_DELAY_TICKS.toLong()
        )

        assertFalse(service().tick(minion))
        assertEquals(Material.AIR, world.typeAt(1, 0, 0))
    }

    /** Every target is seeded, not only the first one listed. */
    @Test
    fun `seeds every configured block`() {
        val world = FakeWorld()
        world.fillFloor()
        val placement = selfSeeding(Material.COAL_ORE).copy(
            seedBlocks = listOf(
                FakeBlockLookup.testable(Material.COAL_ORE),
                FakeBlockLookup.testable(Material.DEEPSLATE_COAL_ORE)
            )
        )
        val minion = minionAt(world, placement = placement, height = 2)
        val service = service()

        repeat(SPOTS_AROUND_MINION) { service.tick(minion) }

        assertTrue(world.countOf(Material.COAL_ORE) > 0)
        assertTrue(world.countOf(Material.DEEPSLATE_COAL_ORE) > 0)
    }

    /** A regen marker is a stand-in, so a plant set on it pops when the regen fires. */
    @Test
    fun `does not seed on top of a spot waiting on regen`() {
        val world = FakeWorld()
        world.fillFloor()
        world.fillLayer(0, Material.COAL_ORE)
        world.set(1, 0, 0, Material.COBBLESTONE)
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 3)
        minion.mining.regenQueue += MiningState.RegenEntry(
            location = world.blockAt(1, 0, 0).location,
            regenData = mockk<BlockData>(),
            surfaceBlock = null,
            regenAt = REGEN_DELAY_TICKS.toLong()
        )

        repeat(SPOTS_AROUND_MINION) { service().tick(minion) }

        assertEquals(Material.AIR, world.typeAt(1, 1, 0))
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

    /** A crop type declares its surface, and the minion lays it on bare ground. */
    @Test
    fun `lays its configured surface under a seeded crop`() {
        val world = FakeWorld()
        world.fillFloor(Material.STONE)
        val minion = minionAt(world, placement = cropSeeding())

        assertTrue(service().tick(minion))
        assertEquals(Material.WHEAT, world.typeAt(-1, 0, 0))
        assertEquals(Material.FARMLAND, world.typeAt(-1, -1, 0))
        assertEquals(1, world.countOf(Material.WHEAT))
        assertEquals(1, world.countOf(Material.FARMLAND))
    }

    /** Ground already prepared is used as-is, ahead of converting anything. */
    @Test
    fun `seeds on existing surface without converting more ground`() {
        val world = FakeWorld()
        world.fillFloor(Material.STONE)
        world.set(1, -1, 0, Material.FARMLAND)
        val minion = minionAt(world, placement = cropSeeding())

        assertTrue(service().tick(minion))
        assertEquals(Material.WHEAT, world.typeAt(1, 0, 0))
        assertEquals(1, world.countOf(Material.FARMLAND))
    }

    /** The surface is only ever laid on solid ground, never in mid-air. */
    @Test
    fun `does not lay a surface over a hole`() {
        val world = FakeWorld()
        val minion = minionAt(world, placement = cropSeeding())

        assertFalse(service().tick(minion))
        assertEquals(0, world.countOf(Material.FARMLAND))
    }

    /** A seeded crop is placed fully grown, ready to mine. */
    @Test
    fun `seeds ageable blocks at max age`() {
        val world = FakeWorld()
        world.fillFloor()
        val crop = mockk<Ageable>(relaxed = true) { every { maximumAge } returns 7 }
        val placed = mockk<Block>(relaxed = true) { every { blockData } returns crop }
        val seed = object : TestableBlock {
            override fun matches(block: Block?): Boolean = false
            override fun place(location: Location): Block = placed
        }
        val minion = minionAt(
            world,
            placement = PlacementSettings(liftRegen = false, surfaceBlock = null, selfSeed = true, seedBlocks = listOf(seed))
        )

        assertTrue(service().tick(minion))
        verify { crop.age = 7 }
        verify { placed.blockData = crop }
    }

    private fun cropSeeding() = PlacementSettings(
        liftRegen = true,
        surfaceBlock = FakeBlockLookup.testable(Material.FARMLAND),
        selfSeed = true,
        seedBlocks = listOf(FakeBlockLookup.testable(Material.WHEAT))
    )

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

        assertFalse(service().tick(minion))
        assertEquals(0, world.countOf(Material.COAL_ORE))
    }

    /** The same spots are fair game once the box is tall enough to mine them back. */
    @Test
    fun `seeds higher up when the height reaches there`() {
        val world = FakeWorld()
        world.fillFloor()
        world.fillLayer(0)
        val minion = minionAt(world, placement = selfSeeding(Material.COAL_ORE), height = 3)

        assertTrue(service().tick(minion))
        assertEquals(Material.COAL_ORE, world.typeAt(-1, 1, 0))
    }

    /** A spacing of 1 keeps the four side neighbours clear, but not the diagonals. */
    @Test
    fun `keeps spaced seeds apart`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.CACTUS, spacing = 1), radius = 2, height = 2)
        val service = service()

        while (service.tick(minion)) {
            // seed until nowhere is left
        }

        val seeded = world.positionsOf(Material.CACTUS)
        assertTrue(seeded.size > 1)
        for (a in seeded) {
            for (b in seeded) {
                if (a == b) continue
                val gap = abs(a.first - b.first) + abs(a.third - b.third)
                assertTrue(gap > 1, "$a and $b are within spacing")
            }
        }
    }

    /** Diagonals don't count towards spacing, so seeds may touch corner to corner. */
    @Test
    fun `seeds diagonally beside a plant`() {
        val world = FakeWorld()
        world.fillFloor()
        world.fillLayer(0, Material.STONE)
        world.set(0, 0, 0, Material.AIR)
        world.set(1, 0, 1, Material.AIR)
        world.set(2, 0, 2, Material.CACTUS)
        val minion = minionAt(world, placement = selfSeeding(Material.CACTUS, spacing = 1), height = 2)

        assertTrue(service().tick(minion))
        assertEquals(Material.CACTUS, world.typeAt(1, 0, 1))
    }

    /** Spacing 0 packs seeds side by side, as before spacing existed. */
    @Test
    fun `packs seeds tightly without spacing`() {
        val world = FakeWorld()
        world.fillFloor()
        val minion = minionAt(world, placement = selfSeeding(Material.CACTUS), height = 2)
        val service = service()

        while (service.tick(minion)) {
            // seed until nowhere is left
        }

        assertEquals(SPOTS_AROUND_MINION, world.countOf(Material.CACTUS))
    }

    /** A plant higher up still counts - spacing is about whole columns, not one layer. */
    @Test
    fun `spacing counts plants at other heights`() {
        val world = FakeWorld()
        world.fillFloor()
        world.set(1, 0, 0, Material.STONE)
        world.set(1, 1, 0, Material.CACTUS)
        val minion = minionAt(world, placement = selfSeeding(Material.CACTUS, spacing = 1), height = 3)
        val service = service()

        while (service.tick(minion)) {
            // seed until nowhere is left
        }

        for ((x, _, z) in world.positionsOf(Material.CACTUS) - Triple(1, 1, 0)) {
            assertTrue(abs(x - 1) + abs(z) > 1, "seeded next to the raised cactus at x=$x z=$z")
        }
    }

    /** Nowhere spaced far enough is not pending, so the minion reports blocked. */
    @Test
    fun `is not pending when every spot is too close`() {
        val world = FakeWorld()
        world.fillFloor()
        world.set(1, 0, 0, Material.CACTUS)
        world.set(-1, 0, 0, Material.CACTUS)
        world.set(0, 0, 1, Material.CACTUS)
        world.set(0, 0, -1, Material.CACTUS)
        val minion = minionAt(world, placement = selfSeeding(Material.CACTUS, spacing = 1), height = 2)

        assertFalse(service().isPending(minion))
    }

    private fun service() = SeedingService()

    private fun selfSeeding(seed: Material, spacing: Int = 0) = PlacementSettings(
        liftRegen = false,
        surfaceBlock = null,
        selfSeed = true,
        seedBlocks = listOf(FakeBlockLookup.testable(seed)),
        spacing = spacing
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

        /** A radius of 1 leaves eight spots around the minion's own column. */
        const val SPOTS_AROUND_MINION = 8
    }
}
