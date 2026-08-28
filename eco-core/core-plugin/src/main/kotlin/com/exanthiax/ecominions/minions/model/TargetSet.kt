package com.exanthiax.ecominions.minions.model

import org.bukkit.Material
import org.bukkit.block.Block

/**
 * The blocks one minion mines.
 *
 * Scanned every tick for every minion - 147 blocks at the default radius and
 * height - so the vanilla case cannot afford a linear walk of TestableBlocks.
 * Definitions that name a plain material are indexed by it and answered from a
 * hash lookup; only the custom-block definitions are scanned, and a server
 * rarely has more than a handful.
 */
class TargetSet(
    /** Every definition this minion targets, in the order they were configured. */
    val definitions: List<MinionBlock>
) {
    private val byMaterial: Map<Material, MinionBlock> =
        definitions.mapNotNull { definition ->
            definition.vanillaMaterial?.let { it to definition }
        }.toMap()

    private val custom: List<MinionBlock> =
        definitions.filter { it.vanillaMaterial == null }

    val isEmpty: Boolean
        get() = definitions.isEmpty()

    /** The configured ids, for display and for the API. */
    val ids: Set<String>
        get() = definitions.map { it.lookup }.toSet()

    fun matches(block: Block): Boolean = definitionFor(block) != null

    /** @return The definition [block] is an instance of, or null if it is not a target. */
    fun definitionFor(block: Block): MinionBlock? =
        byMaterial[block.type] ?: custom.firstOrNull { it.block.matches(block) }

    companion object {
        val EMPTY = TargetSet(emptyList())
    }
}
