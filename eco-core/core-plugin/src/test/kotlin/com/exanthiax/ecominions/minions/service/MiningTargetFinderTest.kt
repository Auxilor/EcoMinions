package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.exanthiax.ecominions.minions.model.GameData
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MiningTargetFinderTest {

    private val finder = MiningTargetFinder(GameData.EMPTY)

    /** The default three-block box is exactly the one every type had before height existed. */
    @Test
    fun `the default height scans one below the minion up to one above`() {
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

    private fun minionAt(
        world: FakeWorld,
        radius: Int = 1,
        height: Int
    ): Minion {
        val type = MinionTypeFixture.build(targetBlocks = setOf(Material.OAK_LOG))
        return mockk<Minion>().also { minion ->
            every { minion.state } returns mockk { every { this@mockk.type } returns type }
            every { minion.stats } returns mockk {
                every { this@mockk.radius } returns radius
                every { this@mockk.height } returns height
            }
            every { minion.entity } returns world.stand()
        }
    }
}
