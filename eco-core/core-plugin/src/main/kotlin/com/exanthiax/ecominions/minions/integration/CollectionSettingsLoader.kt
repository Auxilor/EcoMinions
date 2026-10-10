package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.world.ItemLookup
import com.exanthiax.ecominions.minions.model.CollectionSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads a collector's `target-items` and `collect-xp`. */
class CollectionSettingsLoader(
    private val items: ItemLookup
) {
    fun load(config: Config, context: LoadContext): CollectionSettings =
        CollectionSettings(
            targetItems = config.getStrings("target-items").filter { it.isNotBlank() }.mapNotNull { id ->
                items.lookup(id) ?: context.invalid("target-items", id, "an item or custom item id")
            },
            collectExperience = config.getBoolOrNull("collect-xp") ?: true
        )
}
