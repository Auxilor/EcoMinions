package com.exanthiax.ecominions.minions.model

/** How a minion's placeable item looks. */
data class SpawnItemSpec(
    /** eco item lookup string - a plain material, a custom item, or `player_head texture:<base64>` for a head. */
    val itemLookup: String,
    val displayName: String,
    val lore: List<String>
)

/**
 * How a minion's item is crafted. Absent when crafting is disabled.
 *
 * The grid is eco's flat nine-entry form - the same shape EcoItems and
 * EcoCrafting use - so a recipe reads the same across the suite. Entries are
 * item lookups with an optional amount, and an empty string is an empty slot.
 */
data class CraftingSpec(
    val recipe: List<String>,
    val shapeless: Boolean,
    val permission: String?,
    val giveAmount: Int
) {
    companion object {
        const val GRID_SIZE = 9

        /**
         * @return [entries] padded to a full grid, or null if it cannot be a recipe.
         */
        fun normalise(entries: List<String>): List<String>? {
            if (entries.isEmpty() || entries.size > GRID_SIZE) {
                return null
            }
            if (entries.all { it.isBlank() }) {
                return null
            }
            return entries + List(GRID_SIZE - entries.size) { "" }
        }
    }
}
