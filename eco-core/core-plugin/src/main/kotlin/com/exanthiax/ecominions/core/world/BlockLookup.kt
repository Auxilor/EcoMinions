package com.exanthiax.ecominions.core.world

import com.willfp.eco.core.blocks.Blocks
import com.willfp.eco.core.blocks.TestableBlock
import com.willfp.eco.core.blocks.impl.EmptyTestableBlock
import org.bukkit.block.Block

/**
 * The block-identity operations this plugin needs.
 *
 * An interface because eco's own are statics on [Blocks], which no unit test can
 * call, and because every block this plugin touches may be a custom block from
 * ItemsAdder, Oraxen or Nexo rather than a plain material.
 */
interface BlockLookup {
    /** @return The block [id] names, or null if nothing on this server matches it. */
    fun lookup(id: String): TestableBlock?

    fun isCustom(block: Block): Boolean

    /** @return The id [block] would be written as in config. */
    fun lookupStringOf(block: Block): String

    fun matchesAny(block: Block, candidates: Collection<TestableBlock>): Boolean
}

/** The real thing, delegating to eco. */
class EcoBlockLookup : BlockLookup {
    override fun lookup(id: String): TestableBlock? =
        Blocks.lookup(id).takeIf { it !is EmptyTestableBlock }

    override fun isCustom(block: Block): Boolean = Blocks.isCustomBlock(block)

    override fun lookupStringOf(block: Block): String = Blocks.toLookupString(block)

    override fun matchesAny(block: Block, candidates: Collection<TestableBlock>): Boolean =
        Blocks.matchesAny(block, candidates)
}
