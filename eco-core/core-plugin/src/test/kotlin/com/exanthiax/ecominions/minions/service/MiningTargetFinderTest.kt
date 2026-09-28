package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.TargetSet
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MiningTargetFinderTest {

    private val finder = MiningTargetFinder()

    /** The default three-block box is exactly the one every type had before height existed. */
    @Test
    fun `a height of three scans one below the minion up to one above`() {
        val world = FakeWorld()
        world.set(1, -1, 0, Material.OAK_LOG)

        assertEquals(-1, finder.find(minionAt(world, height = 3))?.y)

        world.set(1, -1, 0, Material.AIR)
        world.set(1, 1, 0, Material.OAK_LOG)

        assertEquals(1, finder.find(minionAt(world, height = 3))?.y)
    }

    /** The top log of a self-seeded tree sits four above the minion, and used to be left floating. */
    @Test
    fun `a taller height reaches the top log of a tree`() {
        val world = FakeWorld()
        world.set(1, 4, 0, Material.OAK_LOG)

        assertEquals(4, finder.find(minionAt(world, height = 6))?.y)
    }

    @Test
    fun `a block above the configured height is out of reach`() {
        val world = FakeWorld()
        world.set(1, 4, 0, Material.OAK_LOG)

        assertNull(finder.find(minionAt(world, height = 3)))
    }

    /** Height only ever grows the box upward - the floor stays one block below. */
    @Test
    fun `height never reaches further down than one block`() {
        val world = FakeWorld()
        world.set(1, -2, 0, Material.OAK_LOG)

        assertNull(finder.find(minionAt(world, height = 12)))
    }

    @Test
    fun `blocks outside the horizontal radius are ignored`() {
        val world = FakeWorld()
        world.set(3, 0, 0, Material.OAK_LOG)

        assertNull(finder.find(minionAt(world, radius = 1, height = 6)))
    }

    /**
     * The regen marker is a real block, and a stone minion's targets include the
     * material it is made of. Mining it would hand the minion free drops on
     * every cycle and the regen delay would pace nothing.
     */
    @Test
    fun `a block waiting on regen is not a target`() {
        val world = FakeWorld()
        world.set(1, 0, 0, Material.OAK_LOG)
        val minion = minionAt(world, height = 3)
        minion.mining.regenQueue += MiningState.RegenEntry(
            location = world.blockAt(1, 0, 0).location,
            regenData = mockk<BlockData>(),
            surfaceBlock = null,
            regenAt = 100L
        )

        assertNull(finder.find(minion))
    }

    private fun minionAt(
        world: FakeWorld,
        radius: Int = 1,
        height: Int
    ): Minion {
        val type = MinionDefinitionFixture.build(
            targetBlocks = TargetSet(
                listOf(
                    MinionBlock.regenDefault("oak", "OAK_LOG", FakeBlockLookup.testable(Material.OAK_LOG))
                        .copy(vanillaMaterial = Material.OAK_LOG)
                )
            )
        )
        return mockk<Minion>().also { minion ->
            every { minion.state } returns mockk { every { this@mockk.definition } returns type }
            every { minion.stats } returns mockk {
                every { this@mockk.radius } returns radius
                every { this@mockk.height } returns height
            }
            every { minion.entity } returns world.stand()
            every { minion.mining } returns MiningState()
        }
    }
}
