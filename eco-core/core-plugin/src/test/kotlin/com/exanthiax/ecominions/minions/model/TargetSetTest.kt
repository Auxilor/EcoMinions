package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TargetSetTest {

    private fun vanilla(id: String, material: Material) = MinionBlock.regenDefault(
        id = id.lowercase(),
        lookup = id,
        block = FakeBlockLookup.testable(material)
    ).copy(vanillaMaterial = material)

    /** A definition eco resolved but no material names, as a custom block would be. */
    private fun custom(id: String, matching: Material) = MinionBlock.regenDefault(
        id = id.lowercase(),
        lookup = id,
        block = FakeBlockLookup.testable(matching)
    )

    private fun blockOf(material: Material): Block = mockk<Block>().also {
        every { it.type } returns material
    }

    private val oak = vanilla("OAK_LOG", Material.OAK_LOG)
    private val stone = vanilla("STONE", Material.STONE)

    @Test
    fun `matches a targeted block`() {
        assertTrue(TargetSet(listOf(oak, stone)).matches(blockOf(Material.OAK_LOG)))
    }

    @Test
    fun `does not match an untargeted block`() {
        assertFalse(TargetSet(listOf(oak)).matches(blockOf(Material.STONE)))
    }

    @Test
    fun `resolves the definition for a targeted block`() {
        assertSame(oak, TargetSet(listOf(oak, stone)).definitionFor(blockOf(Material.OAK_LOG)))
    }

    @Test
    fun `has no definition for an untargeted block`() {
        assertNull(TargetSet(listOf(oak)).definitionFor(blockOf(Material.STONE)))
    }

    @Test
    fun `matches a custom block that no material names`() {
        val ruby = custom("itemsadder:ruby_ore", Material.DIAMOND_ORE)

        assertSame(ruby, TargetSet(listOf(ruby)).definitionFor(blockOf(Material.DIAMOND_ORE)))
    }

    /** A custom block is drawn with a vanilla block state, so its material alone must not win. */
    @Test
    fun `prefers a custom block over the vanilla material it is drawn with`() {
        val customStone = custom("plugin:fake_stone", Material.STONE)

        assertSame(customStone, TargetSet(listOf(stone, customStone)).definitionFor(blockOf(Material.STONE)))
    }

    @Test
    fun `an empty set matches nothing`() {
        assertFalse(TargetSet.EMPTY.matches(blockOf(Material.STONE)))
        assertTrue(TargetSet.EMPTY.isEmpty)
    }

    @Test
    fun `exposes the ids it targets`() {
        assertEquals(setOf("OAK_LOG", "STONE"), TargetSet(listOf(oak, stone)).ids)
    }
}
