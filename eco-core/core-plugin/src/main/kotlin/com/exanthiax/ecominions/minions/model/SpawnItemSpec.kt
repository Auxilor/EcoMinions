package com.exanthiax.ecominions.minions.model

/** How a minion type's placeable item looks. */
data class SpawnItemSpec(
    val mode: Mode,
    /** Base64 skin texture, used when [mode] is [Mode.HEAD]. */
    val texture: String,
    /** eco item lookup string, used when [mode] is [Mode.ITEM]. */
    val itemLookup: String,
    val displayName: String,
    val lore: List<String>
) {
    enum class Mode { HEAD, ITEM }
}

/** How a minion type's item is crafted. Absent when crafting is disabled. */
data class CraftingSpec(
    val shape: List<String>,
    val ingredients: Map<Char, String>,
    val requiresCraftingTable: Boolean,
    val permission: String
)
