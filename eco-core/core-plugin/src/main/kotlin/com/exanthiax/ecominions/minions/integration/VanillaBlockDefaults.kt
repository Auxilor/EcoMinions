package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.world.BlockLookup
import com.exanthiax.ecominions.minions.model.MinionBlock
import org.bukkit.Material

/**
 * What the plugin knows about vanilla blocks without being told.
 *
 * Oak grows from a sapling under leaves; wheat sits on farmland; cactus is cut
 * at its base. These are facts about Minecraft, not decisions a server makes,
 * so they live in code and `blocks/` stays empty until someone has a custom
 * block to describe or a default to change.
 *
 * A definition in `blocks/` with the same block id wins over anything here, so
 * every one of these remains overridable without touching the jar.
 *
 * Resolved lazily and through [BlockLookup]: a block this server's version does
 * not have is skipped rather than failing the load.
 */
class VanillaBlockDefaults(
    private val blocks: BlockLookup
) {
    private val byLookup: Map<String, MinionBlock> by lazy { build() }

    /** @return The built-in definition for [lookup], or null if there is none. */
    fun forId(lookup: String): MinionBlock? = byLookup[lookup]

    fun all(): Collection<MinionBlock> = byLookup.values

    private fun build(): Map<String, MinionBlock> {
        val definitions = TREES.mapNotNull { tree(it) } +
            CROPS.mapNotNull { crop(it) } +
            COLUMNS.mapNotNull { column(it) } +
            GROUNDED.mapNotNull { grounded(it) } +
            PROMOTIONS.mapNotNull { promotion(it) } +
            UNMINEABLE.mapNotNull { unmineable(it) } +
            ORES.mapNotNull { ore(it) }

        return definitions.associateBy { it.lookup }
    }

    /** A tree species: a log that fells to a sapling, with a canopy to harvest. */
    private fun tree(spec: Tree): MinionBlock? = define(spec.id, spec.log) {
        it.copy(
            plant = blocks.lookup(spec.sapling),
            leaves = blocks.lookup(spec.leaves),
            surface = blocks.lookup(spec.surface)
        )
    }

    /** A crop: harvested ripe, replanted as itself, and fussy about what is beneath. */
    private fun crop(spec: Crop): MinionBlock? = define(spec.id, spec.block) {
        it.copy(
            plant = blocks.lookup(spec.block),
            surface = blocks.lookup(spec.surface)
        )
    }

    /** A plant that grows upward and is only ever cut at its base. */
    private fun column(spec: Column): MinionBlock? =
        define(spec.block.lowercase(), spec.block) {
            it.copy(stackedColumn = true, surface = spec.surface?.let(blocks::lookup))
        }

    /** A plant that only survives on one kind of ground. */
    private fun grounded(spec: Grounded): MinionBlock? =
        define(spec.block.lowercase(), spec.block) { it.copy(surface = blocks.lookup(spec.surface)) }

    private fun promotion(spec: Promotion): MinionBlock? = define(spec.id, spec.block) {
        it.copy(promotesTo = spec.becomes, stackedColumn = spec.stackedColumn)
    }

    /** A block that breaks badly enough that no minion should be pointed at it. */
    private fun unmineable(lookup: String): MinionBlock? =
        define(lookup.lowercase(), lookup) { it.copy(mineable = false) }

    /** An ore: regenerates in place and is worth the XP vanilla drops for it. */
    private fun ore(spec: Ore): MinionBlock? =
        define(spec.block.lowercase(), spec.block) { it.copy(experience = spec.experience) }

    private fun define(id: String, lookup: String, fill: (MinionBlock) -> MinionBlock): MinionBlock? {
        val block = blocks.lookup(lookup) ?: return null
        val base = MinionBlock.regenDefault(id, lookup, block)
            .copy(vanillaMaterial = Material.matchMaterial(lookup)?.takeIf { it.isBlock })
        return fill(base)
    }

    private data class Tree(
        val id: String,
        val log: String,
        val sapling: String,
        val leaves: String,
        val surface: String = "GRASS_BLOCK"
    )

    private data class Crop(val id: String, val block: String, val surface: String)

    private data class Column(val block: String, val surface: String? = null)

    private data class Grounded(val block: String, val surface: String)

    private data class Ore(val block: String, val experience: IntRange)

    private data class Promotion(
        val id: String,
        val block: String,
        val becomes: String,
        val stackedColumn: Boolean = false
    )

    private companion object {
        /**
         * Listed rather than derived from the log's name: mangrove grows from a
         * propagule on mud, and a naming rule that holds for eight species and
         * breaks on the ninth is worse than a table.
         */
        val TREES = listOf(
            Tree("oak", "OAK_LOG", "OAK_SAPLING", "OAK_LEAVES"),
            Tree("birch", "BIRCH_LOG", "BIRCH_SAPLING", "BIRCH_LEAVES"),
            Tree("spruce", "SPRUCE_LOG", "SPRUCE_SAPLING", "SPRUCE_LEAVES"),
            Tree("jungle", "JUNGLE_LOG", "JUNGLE_SAPLING", "JUNGLE_LEAVES"),
            Tree("acacia", "ACACIA_LOG", "ACACIA_SAPLING", "ACACIA_LEAVES"),
            Tree("dark_oak", "DARK_OAK_LOG", "DARK_OAK_SAPLING", "DARK_OAK_LEAVES"),
            Tree("cherry", "CHERRY_LOG", "CHERRY_SAPLING", "CHERRY_LEAVES"),
            Tree("pale_oak", "PALE_OAK_LOG", "PALE_OAK_SAPLING", "PALE_OAK_LEAVES"),
            Tree("mangrove", "MANGROVE_LOG", "MANGROVE_PROPAGULE", "MANGROVE_LEAVES", surface = "MUD")
        )

        val CROPS = listOf(
            Crop("wheat", "WHEAT", "FARMLAND"),
            Crop("carrots", "CARROTS", "FARMLAND"),
            Crop("potatoes", "POTATOES", "FARMLAND"),
            Crop("beetroots", "BEETROOTS", "FARMLAND"),
            Crop("nether_wart", "NETHER_WART", "SOUL_SAND")
        )

        val COLUMNS = listOf(
            Column("SUGAR_CANE"),
            Column("CACTUS", surface = "SAND"),
            Column("BAMBOO"),
            Column("KELP")
        )

        /** Mycelium rather than podzol: mushrooms on it grow in any light. */
        val GROUNDED = listOf(
            Grounded("RED_MUSHROOM", "MYCELIUM"),
            Grounded("BROWN_MUSHROOM", "MYCELIUM")
        )

        val PROMOTIONS = listOf(
            Promotion("bamboo_sapling", "BAMBOO_SAPLING", "BAMBOO"),
            Promotion("kelp_plant", "KELP_PLANT", "KELP", stackedColumn = true)
        )

        /**
         * Amethyst buds are here rather than promoting to the cluster: the
         * bundled data used to do both, and promoting one unmineable block to
         * another unmineable block says the same thing twice.
         */
        val UNMINEABLE = listOf(
            "SMALL_AMETHYST_BUD",
            "MEDIUM_AMETHYST_BUD",
            "LARGE_AMETHYST_BUD",
            "AMETHYST_CLUSTER",
            "POINTED_DRIPSTONE",
            "GLOW_LICHEN",
            "SCULK_VEIN",
            "VINE",
            "TWISTING_VINES",
            "TWISTING_VINES_PLANT",
            "WEEPING_VINES",
            "WEEPING_VINES_PLANT"
        )

        /** Vanilla's XP per ore. Deepslate variants drop the same as their stone ones. */
        val ORES = listOf(
            Ore("COAL_ORE", 0..2),
            Ore("DEEPSLATE_COAL_ORE", 0..2),
            Ore("DIAMOND_ORE", 3..7),
            Ore("DEEPSLATE_DIAMOND_ORE", 3..7),
            Ore("EMERALD_ORE", 3..7),
            Ore("DEEPSLATE_EMERALD_ORE", 3..7),
            Ore("LAPIS_ORE", 2..5),
            Ore("DEEPSLATE_LAPIS_ORE", 2..5),
            Ore("REDSTONE_ORE", 1..5),
            Ore("DEEPSLATE_REDSTONE_ORE", 1..5),
            Ore("NETHER_QUARTZ_ORE", 2..5),
            Ore("NETHER_GOLD_ORE", 0..1),
            Ore("SCULK", 1..1)
        )
    }
}
