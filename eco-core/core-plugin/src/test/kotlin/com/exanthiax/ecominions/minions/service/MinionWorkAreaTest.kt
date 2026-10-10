package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionWorkAreaTest {
    @Test
    fun `blocks covers the box and skips the minion's column`() {
        val world = FakeWorld()
        val type = MinionDefinitionFixture.build(
            baseStats = MinionDefinitionFixture.build().baseStats.copy(floorOffset = 0)
        )
        val minion = mockk<Minion> {
            every { state } returns mockk { every { definition } returns type }
            every { stats } returns mockk {
                every { radius } returns 1
                every { height } returns 2
            }
            every { entity } returns world.stand()
        }

        val blocks = MinionWorkArea.blocks(minion)

        assertEquals(16, blocks.size)
        assertTrue(blocks.none { it.x == 0 && it.z == 0 })
        assertEquals(setOf(0, 1), blocks.map { it.y }.toSet())
    }
}
