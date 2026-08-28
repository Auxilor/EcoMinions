package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionBlockTest {

    @Test
    fun `a regen default overrides nothing and plants nothing`() {
        val definition = MinionBlock.regenDefault(
            id = "diamond_ore",
            lookup = "DIAMOND_ORE",
            block = FakeBlockLookup.testable(Material.DIAMOND_ORE)
        )

        assertNull(definition.restore)
        assertFalse(definition.isPlantable)
    }

    @Test
    fun `a definition with a plant is plantable`() {
        val definition = MinionBlock(
            id = "oak",
            lookup = "OAK_LOG",
            block = FakeBlockLookup.testable(Material.OAK_LOG),
            restore = RestoreMode.TREE,
            plant = FakeBlockLookup.testable(Material.OAK_SAPLING),
            leaves = FakeBlockLookup.testable(Material.OAK_LEAVES),
            surface = FakeBlockLookup.testable(Material.GRASS_BLOCK),
            treePattern = null,
            drops = emptyList()
        )

        assertTrue(definition.isPlantable)
        assertEquals("oak", definition.patternKey)
    }

    @Test
    fun `an explicit tree pattern wins over the id`() {
        val definition = MinionBlock(
            id = "ruby_tree",
            lookup = "itemsadder:ruby_log",
            block = FakeBlockLookup.testable(Material.OAK_LOG),
            restore = RestoreMode.TREE,
            plant = FakeBlockLookup.testable(Material.OAK_SAPLING),
            leaves = FakeBlockLookup.testable(Material.OAK_LEAVES),
            surface = null,
            treePattern = "spruce",
            drops = emptyList()
        )

        assertEquals("spruce", definition.patternKey)
    }
}
