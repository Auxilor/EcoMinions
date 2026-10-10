package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.config.IntRanges
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.core.world.BlockLookup
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.RestoreMode
import com.willfp.eco.core.blocks.TestableBlock
import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Material

/**
 * Parses one file in `blocks/` into a validated [MinionBlock].
 *
 * A file that names a block this server does not have is rejected outright; an
 * incomplete `restore:` override is dropped rather than repaired, and the
 * minion's own type decides instead.
 */
class MinionBlockLoader(
    private val blocks: BlockLookup
) : ConfigLoader<MinionBlock> {

    override fun load(id: String, config: Config, context: LoadContext): MinionBlock? {
        val lookup = config.getStringOrNull("block")?.takeIf { it.isNotBlank() }
            ?: return context.invalid("block", "", "a block or custom block id")
        val block = blocks.lookup(lookup)
            ?: return context.invalid("block", lookup, "a block or custom block id")

        val plant = readBlock(config, "plant", context)
        val leaves = readBlock(config, "leaves", context)
        val surface = readBlock(config, "surface", context)
        val restore = readRestore(id, config, plant, leaves, context)

        return MinionBlock(
            id = id,
            lookup = lookup,
            block = block,
            restore = restore,
            plant = plant,
            leaves = leaves,
            surface = surface,
            treePattern = config.getStringOrNull("tree-pattern")?.takeIf { it.isNotBlank() },
            drops = config.getStrings("drops"),
            mineable = config.getBoolOrNull("mineable") ?: true,
            stackedColumn = config.getBoolOrNull("stacked-column") ?: false,
            promotesTo = readPromotion(config, context),
            vanillaMaterial = Material.matchMaterial(lookup)?.takeIf { it.isBlock },
            experience = readExperience(config, context)
        )
    }

    /**
     * @return The override this block declares, or null to leave it to the minion.
     *
     * An override that asks for something the definition cannot do is dropped
     * rather than downgraded: the minion's type is a better guess than a
     * half-honoured override.
     */
    private fun readRestore(
        id: String,
        config: Config,
        plant: TestableBlock?,
        leaves: TestableBlock?,
        context: LoadContext
    ): RestoreMode? {
        val raw = config.getStringOrNull("restore")?.takeIf { it.isNotBlank() } ?: return null
        val mode = RestoreMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        if (mode == null) {
            return context.unknown("restore", raw, RestoreMode.entries.map { it.name.lowercase() })
        }

        if (mode == RestoreMode.TREE && (plant == null || leaves == null)) {
            context.violation(
                "restore",
                "'$id' grows a tree but has no ${if (plant == null) "plant" else "leaves"} - " +
                    "leaving it to the minion's type"
            )
            return null
        }

        if (mode == RestoreMode.REPLANT && plant == null) {
            context.violation("restore", "'$id' replants but has no plant - leaving it to the minion's type")
            return null
        }

        return mode
    }

    /**
     * @return The id this block promotes to, validated against this server.
     *
     * Kept as a string rather than the resolved block: it is looked up against
     * the `blocks/` catalog later, and resolving it here would only prove that
     * eco knows it, which is what this check already does.
     */
    private fun readExperience(config: Config, context: LoadContext): IntRange? {
        val raw = config.getStringOrNull("xp")?.takeIf { it.isNotBlank() } ?: return null
        return IntRanges.parse(raw) ?: context.invalid("xp", raw, "a whole number or a range like 2-5")
    }

    private fun readPromotion(config: Config, context: LoadContext): String? {
        val raw = config.getStringOrNull("promotes-to")?.takeIf { it.isNotBlank() } ?: return null
        if (blocks.lookup(raw) == null) {
            return context.invalid("promotes-to", raw, "a block or custom block id")
        }
        return raw
    }

    private fun readBlock(config: Config, param: String, context: LoadContext): TestableBlock? {
        val raw = config.getStringOrNull(param)?.takeIf { it.isNotBlank() } ?: return null
        return blocks.lookup(raw) ?: context.invalid(param, raw, "a block or custom block id")
    }
}
