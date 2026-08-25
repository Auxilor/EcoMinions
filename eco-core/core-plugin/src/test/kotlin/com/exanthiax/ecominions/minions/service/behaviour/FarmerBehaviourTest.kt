package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmerBehaviourTest {

    private val regen = mockk<RegenService>(relaxed = true)
    private val seeding = mockk<SeedingService>()
    private val behaviour = FarmerBehaviour(regen, seeding)
    private val minion = mockk<Minion>()

    private val wheat = MinionBlock.regenDefault("wheat", "WHEAT", FakeBlockLookup.testable(Material.WHEAT))
        .copy(plant = FakeBlockLookup.testable(Material.WHEAT))

    @Test
    fun `replants the crop rather than queueing a regen`() {
        val world = FakeWorld()
        val block = world.blockAt(0, 0, 0)
        block.type = Material.AIR

        behaviour.onBlockBroken(minion, block, wheat, Material.WHEAT, now = 100L)

        assertEquals(Material.WHEAT, block.type, "the crop should be replanted immediately")
        verify(exactly = 0) { regen.queue(any(), any(), any(), any()) }
    }

    @Test
    fun `regenerates when the block names nothing to plant`() {
        val bare = MinionBlock.regenDefault("stone", "STONE", FakeBlockLookup.testable(Material.STONE))
        val block = FakeWorld().blockAt(0, 0, 0)

        behaviour.onBlockBroken(minion, block, bare, Material.STONE, now = 100L)

        verify { regen.queue(minion, block, Material.STONE, 100L) }
    }

    @Test
    fun `regenerates when nothing defines the block at all`() {
        val block = FakeWorld().blockAt(0, 0, 0)

        behaviour.onBlockBroken(minion, block, null, Material.STONE, now = 100L)

        verify { regen.queue(minion, block, Material.STONE, 100L) }
    }

    @Test
    fun `is busy when it seeded a block`() {
        every { seeding.tick(minion) } returns true

        assertTrue(behaviour.onIdle(minion, now = 0L))
    }

    @Test
    fun `is not busy when it could not seed`() {
        every { seeding.tick(minion) } returns false

        assertFalse(behaviour.onIdle(minion, now = 0L))
    }
}
