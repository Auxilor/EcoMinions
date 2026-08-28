package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.block.Block
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinerBehaviourTest {

    private val regen = mockk<RegenService>(relaxed = true)
    private val seeding = mockk<SeedingService>()
    private val behaviour = MinerBehaviour(regen, seeding)

    @Test
    fun `queues a regen for the block it broke`() {
        val minion = mockk<Minion>()
        val block = mockk<Block>()

        behaviour.onBlockBroken(minion, block, definition = null, original = Material.STONE, now = 100L)

        verify { regen.queue(minion, block, Material.STONE, 100L) }
    }

    @Test
    fun `regenerates even when the block could be replanted`() {
        val minion = mockk<Minion>()
        val block = mockk<Block>()
        val wheat = MinionBlock.regenDefault("wheat", "WHEAT", FakeBlockLookup.testable(Material.WHEAT))
            .copy(plant = FakeBlockLookup.testable(Material.WHEAT))

        behaviour.onBlockBroken(minion, block, wheat, Material.WHEAT, now = 100L)

        verify { regen.queue(minion, block, Material.WHEAT, 100L) }
    }

    /** Seeding takes the cycle, so the ore it just placed is not mined on the same one. */
    @Test
    fun `takes the cycle when it seeded`() {
        val minion = mockk<Minion>()
        every { seeding.tick(minion) } returns true

        assertTrue(behaviour.onBeforeMine(minion, now = 0L))
    }

    @Test
    fun `leaves the cycle to mining when nothing is left to seed`() {
        val minion = mockk<Minion>()
        every { seeding.tick(minion) } returns false

        assertFalse(behaviour.onBeforeMine(minion, now = 0L))
    }

    @Test
    fun `is busy while a seed is still on the way`() {
        val minion = mockk<Minion>()
        every { seeding.isPending(minion) } returns true

        assertTrue(behaviour.onIdle(minion, now = 0L))
    }

    @Test
    fun `is blocked when there is nothing left to seed`() {
        val minion = mockk<Minion>()
        every { seeding.isPending(minion) } returns false

        assertFalse(behaviour.onIdle(minion, now = 0L))
    }
}
