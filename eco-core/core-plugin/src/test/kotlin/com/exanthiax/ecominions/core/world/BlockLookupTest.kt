package com.exanthiax.ecominions.core.world

import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.willfp.eco.core.blocks.TestableBlock
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockLookupTest {

    @Test
    fun `resolves a registered id`() {
        val oak = FakeBlockLookup.testable(Material.OAK_LOG)
        val lookup = FakeBlockLookup(mapOf("OAK_LOG" to oak))

        assertSame(oak, lookup.lookup("OAK_LOG"))
    }

    @Test
    fun `returns null for an unresolvable id`() {
        assertNull(FakeBlockLookup().lookup("NOT_A_BLOCK"))
    }

    @Test
    fun `matches any of the given candidates`() {
        val oak: TestableBlock = FakeBlockLookup.testable(Material.OAK_LOG)
        val birch: TestableBlock = FakeBlockLookup.testable(Material.BIRCH_LOG)
        val lookup = FakeBlockLookup()
        val block = mockk<Block>()
        every { block.type } returns Material.BIRCH_LOG

        assertTrue(lookup.matchesAny(block, listOf(oak, birch)))
    }

    @Test
    fun `does not match when no candidate matches`() {
        val oak: TestableBlock = FakeBlockLookup.testable(Material.OAK_LOG)
        val lookup = FakeBlockLookup()
        val block = mockk<Block>()
        every { block.type } returns Material.STONE

        assertFalse(lookup.matchesAny(block, listOf(oak)))
    }
}
