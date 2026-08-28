package com.exanthiax.ecominions.testsupport

import com.exanthiax.ecominions.core.world.BlockLookup
import com.willfp.eco.core.blocks.TestableBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * An in-memory [BlockLookup] for tests.
 *
 * Matching is by [Block.type] only, which is all the pure layers need: nothing
 * below the eco seam knows what a custom block is.
 */
class FakeBlockLookup(
    private val registered: Map<String, TestableBlock> = emptyMap()
) : BlockLookup {

    override fun lookup(id: String): TestableBlock? = registered[id]

    companion object {
        /** A [TestableBlock] that matches, and places, one material. */
        fun testable(material: Material): TestableBlock = MaterialFake(material)
    }

    private class MaterialFake(private val material: Material) : TestableBlock {
        override fun matches(block: Block?): Boolean = block?.type == material

        override fun place(location: Location): Block {
            val block = location.block
            block.type = material
            return block
        }
    }
}
