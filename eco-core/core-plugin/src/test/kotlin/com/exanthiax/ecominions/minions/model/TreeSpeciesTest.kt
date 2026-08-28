package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.exanthiax.ecominions.minions.integration.VanillaBlockDefaults
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the species table itself.
 *
 * Its predecessor was a bundled json whose species map shipped empty for long
 * enough that every tree grew the same stub, and nothing failed - the lookup
 * just missed and fell back to the default. Only a test over the table catches
 * that, wherever the table lives.
 */
class TreeSpeciesTest {

    /** Every tree [VanillaBlockDefaults] knows, taken from the source of truth rather than retyped. */
    private val vanillaTrees: List<String> =
        VanillaBlockDefaults(everyBlockResolves()).all()
            .filter { it.plant != null && it.leaves != null }
            .map { it.id }

    @Test
    fun `every vanilla tree species has its own shape`() {
        val missing = vanillaTrees.filterNot { TreeShape.forSpecies(it) != null }

        assertTrue(missing.isEmpty(), "these species would silently grow the generic shape: $missing")
    }

    @Test
    fun `the species are not all the same tree`() {
        assertTrue(
            TreeShape.SPECIES.values.toSet().size > 1,
            "every species resolves to one shape - this is the empty-species-map bug"
        )
    }

    @Test
    fun `no species grows a trunk its own minion cannot reach`() {
        for ((id, shape) in TreeShape.SPECIES) {
            assertTrue(
                shape.trunk <= TreeShape.MAX_REACHABLE_TRUNK,
                "'$id' grows a ${shape.trunk}-log trunk, which a minion on the default height cannot fell"
            )
        }
    }

    @Test
    fun `lookup ignores case`() {
        assertEquals(TreeShape.forSpecies("oak"), TreeShape.forSpecies("OAK"))
    }

    @Test
    fun `an unknown species has no shape, so the caller falls back`() {
        assertNull(TreeShape.forSpecies("ebony"))
    }

    @Test
    fun `spruce is the conifer`() {
        assertEquals(TreeShape.Style.CONIFER, TreeShape.forSpecies("spruce")?.style)
    }

    @Test
    fun `acacia is the flat one`() {
        assertEquals(TreeShape.Style.FLAT, TreeShape.forSpecies("acacia")?.style)
    }

    @Test
    fun `every shape places at least one log and one leaf`() {
        for ((id, shape) in TreeShape.SPECIES) {
            val blocks = shape.blocks()
            assertNotNull(blocks.firstOrNull { it.kind == TreeBlock.Kind.LOG }, "'$id' has no trunk")
            assertNotNull(blocks.firstOrNull { it.kind == TreeBlock.Kind.LEAF }, "'$id' has no canopy")
        }
    }

    /** A lookup that resolves anything, so the defaults table is fully populated. */
    private fun everyBlockResolves() = object : com.exanthiax.ecominions.core.world.BlockLookup {
        override fun lookup(id: String) = FakeBlockLookup.testable(org.bukkit.Material.STONE)
    }
}
