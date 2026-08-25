package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
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

        behaviour.onBlockBroken(minion, block, Material.STONE, now = 100L)

        verify { regen.queue(minion, block, Material.STONE, 100L) }
    }

    @Test
    fun `is busy when it seeded a block`() {
        val minion = mockk<Minion>()
        every { seeding.tick(minion) } returns true

        assertTrue(behaviour.onIdle(minion, now = 0L))
    }

    @Test
    fun `is not busy when it could not seed`() {
        val minion = mockk<Minion>()
        every { seeding.tick(minion) } returns false

        assertFalse(behaviour.onIdle(minion, now = 0L))
    }
}
