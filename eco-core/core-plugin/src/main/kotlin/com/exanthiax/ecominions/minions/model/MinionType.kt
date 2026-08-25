package com.exanthiax.ecominions.minions.model

/**
 * What a minion does with the blocks it breaks.
 *
 * A fixed set rather than a config category, because the behaviour is code: a
 * woodcutter's plant-wait-fell cycle cannot be written in yml. Adding a type
 * later is additive - a new constant and a new behaviour class.
 */
enum class MinionType {
    /** Breaks and regenerates in place. The default for anything that is not grown. */
    MINER,

    /** Fells the tree, plants a sapling, and waits for it to grow. */
    WOODCUTTER,

    /** Harvests a ripe crop and replants it. */
    FARMER;

    /** How this type is spelled in a minion's config file. */
    val id: String
        get() = name.lowercase()

    companion object {
        /** @return The type [raw] names, or null if it is missing, blank or unknown. */
        fun parse(raw: String?): MinionType? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        }
    }
}
