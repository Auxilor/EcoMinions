package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.integration.VanillaBlockDefaults
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionBlockIndexTest {

    /** No vanilla defaults, so each test sees only what it declares. */
    private fun index(definitions: List<MinionBlock>) =
        MinionBlockIndex({ definitions }, VanillaBlockDefaults(FakeBlockLookup()))


    private val oak = MinionBlock.regenDefault("oak", "OAK_LOG", FakeBlockLookup.testable(Material.OAK_LOG))
    private val wheat = MinionBlock.regenDefault("wheat", "WHEAT", FakeBlockLookup.testable(Material.WHEAT))

    @Test
    fun `finds a definition by lookup`() {
        val index = index(listOf(oak, wheat))

        assertEquals(oak, index.forLookup("OAK_LOG"))
    }

    @Test
    fun `returns null for an undefined lookup`() {
        assertNull(index(listOf(oak)).forLookup("DIAMOND_ORE"))
    }

    @Test
    fun `finds a definition for a world block`() {
        val index = index(listOf(oak, wheat))

        assertEquals(wheat, index.forBlock(blockOf(Material.WHEAT)))
    }

    @Test
    fun `returns null for a world block nothing defines`() {
        assertNull(index(listOf(oak)).forBlock(blockOf(Material.STONE)))
    }

    @Test
    fun `sees definitions added after construction`() {
        var definitions = listOf(oak)
        val index = MinionBlockIndex({ definitions }, VanillaBlockDefaults(FakeBlockLookup()))
        assertNull(index.forLookup("WHEAT"))

        definitions = listOf(oak, wheat)
        index.invalidate()

        assertEquals(wheat, index.forLookup("WHEAT"))
    }

    @Test
    fun `falls back to a built-in definition when nothing authored defines the block`() {
        val defaults = VanillaBlockDefaults(
            FakeBlockLookup(
                mapOf(
                    "OAK_LOG" to FakeBlockLookup.testable(Material.OAK_LOG),
                    "OAK_SAPLING" to FakeBlockLookup.testable(Material.OAK_SAPLING),
                    "OAK_LEAVES" to FakeBlockLookup.testable(Material.OAK_LEAVES)
                )
            )
        )
        val index = MinionBlockIndex({ emptyList() }, defaults)

        assertNotNull(index.forLookup("OAK_LOG")?.plant, "the built-in oak should carry its sapling")
    }

    @Test
    fun `an authored definition overrides the built-in one`() {
        val defaults = VanillaBlockDefaults(
            FakeBlockLookup(mapOf("OAK_LOG" to FakeBlockLookup.testable(Material.OAK_LOG)))
        )
        val index = MinionBlockIndex({ listOf(oak) }, defaults)

        assertEquals(oak, index.forLookup("OAK_LOG"))
    }

    private fun blockOf(material: Material): Block {
        val block = mockk<Block>()
        every { block.type } returns material
        return block
    }
}
