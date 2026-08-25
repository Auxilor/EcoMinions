package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.MinionBlock
import org.bukkit.block.Block

/**
 * Finds the `blocks/` definition that describes a block.
 *
 * Two questions get asked, and they are not the same one. Loading a minion
 * resolves a configured id; mining resolves a block in the world, which may have
 * been broken by a libreforge effect and so carries no id at all.
 *
 * Definitions are read through a lambda rather than held, so this survives a
 * reload and does not care which config category loaded first.
 */
class MinionBlockIndex(
    private val definitions: () -> Collection<MinionBlock>
) {
    private var byLookup: Map<String, MinionBlock>? = null

    /** @return The definition for a configured id, or null if nothing defines it. */
    fun forLookup(lookup: String): MinionBlock? = index()[lookup]

    /**
     * @return The definition matching a block in the world.
     *
     * Walks the definitions rather than reading the block's id: a custom block
     * only answers to its owning plugin's own test.
     */
    fun forBlock(block: Block): MinionBlock? =
        definitions().firstOrNull { it.block.matches(block) }

    /** Drops the cached index, so the next read sees a reloaded catalog. */
    fun invalidate() {
        byLookup = null
    }

    private fun index(): Map<String, MinionBlock> =
        byLookup ?: definitions().associateBy { it.lookup }.also { byLookup = it }
}
