package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.integration.VanillaBlockDefaults
import com.exanthiax.ecominions.minions.model.MinionBlock
import org.bukkit.Material
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
    private val definitions: () -> Collection<MinionBlock>,
    /** What the plugin knows without being told. A `blocks/` file with the same id wins. */
    private val defaults: VanillaBlockDefaults
) {
    private var index: Index? = null

    /** @return The definition for a configured id, or null if nothing defines it. */
    fun forLookup(lookup: String): MinionBlock? = index().byLookup[lookup]

    /**
     * @return The definition matching a block in the world.
     *
     * Read on every break, so the vanilla case is a hash lookup. Only custom
     * definitions are walked, because a custom block answers to nothing but its
     * owning plugin's own test.
     */
    fun forBlock(block: Block): MinionBlock? {
        val current = index()
        return current.byMaterial[block.type]
            ?: current.custom.firstOrNull { it.block.matches(block) }
    }

    /** Drops the cached index, so the next read sees a reloaded catalog. */
    fun invalidate() {
        index = null
    }

    private fun index(): Index = index ?: build().also { index = it }

    private fun build(): Index {
        // Defaults first: associateBy keeps the last entry, so an authored
        // definition overrides the built-in one for the same block.
        val all = defaults.all() + definitions()
        return Index(
            byLookup = all.associateBy { it.lookup },
            byMaterial = all.mapNotNull { definition ->
                definition.vanillaMaterial?.let { it to definition }
            }.toMap(),
            custom = all.filter { it.vanillaMaterial == null }
        )
    }

    private class Index(
        val byLookup: Map<String, MinionBlock>,
        val byMaterial: Map<Material, MinionBlock>,
        val custom: List<MinionBlock>
    )
}
