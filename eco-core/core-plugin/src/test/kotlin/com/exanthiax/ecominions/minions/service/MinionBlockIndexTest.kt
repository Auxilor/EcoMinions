package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionBlockIndexTest {

    private val oak = MinionBlock.regenDefault("oak", "OAK_LOG", FakeBlockLookup.testable(Material.OAK_LOG))
    private val wheat = MinionBlock.regenDefault("wheat", "WHEAT", FakeBlockLookup.testable(Material.WHEAT))

    @Test
    fun `finds a definition by lookup`() {
        val index = MinionBlockIndex { listOf(oak, wheat) }

        assertEquals(oak, index.forLookup("OAK_LOG"))
    }

    @Test
    fun `returns null for an undefined lookup`() {
        assertNull(MinionBlockIndex { listOf(oak) }.forLookup("DIAMOND_ORE"))
    }

    @Test
    fun `finds a definition for a world block`() {
        val index = MinionBlockIndex { listOf(oak, wheat) }

        assertEquals(wheat, index.forBlock(blockOf(Material.WHEAT)))
    }

    @Test
    fun `returns null for a world block nothing defines`() {
        assertNull(MinionBlockIndex { listOf(oak) }.forBlock(blockOf(Material.STONE)))
    }

    @Test
    fun `sees definitions added after construction`() {
        var definitions = listOf(oak)
        val index = MinionBlockIndex { definitions }
        assertNull(index.forLookup("WHEAT"))

        definitions = listOf(oak, wheat)
        index.invalidate()

        assertEquals(wheat, index.forLookup("WHEAT"))
    }

    private fun blockOf(material: Material): Block {
        val block = mockk<Block>()
        every { block.type } returns material
        return block
    }
}
