package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.world.EntityLookup
import com.exanthiax.ecominions.core.world.ItemLookup
import com.exanthiax.ecominions.minions.model.BreederSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads a breeder's `target-mobs`, `food` and `max-mobs`. */
class BreederSettingsLoader(
    private val entities: EntityLookup,
    private val items: ItemLookup
) {
    fun load(config: Config, context: LoadContext): BreederSettings? =
        resolve(config, context, "target-mobs", MOBS, "a mob or custom mob id", entities::lookup)?.let { targets ->
            resolve(config, context, "food", FOOD, "an item or custom item id", items::lookup)?.let { food ->
                BreederSettings(targets = targets, food = food, maxMobs = maxMobsOf(config))
            }
        }

    private fun maxMobsOf(config: Config): String =
        config.getStringOrNull("max-mobs")?.takeIf { it.isNotBlank() } ?: DEFAULT_MAX_MOBS

    private fun <T : Any> resolve(
        config: Config,
        context: LoadContext,
        key: String,
        names: Names,
        expected: String,
        lookup: (String) -> T?
    ): List<T>? {
        val configured = config.getStrings(key).filter { it.isNotBlank() }
        val resolved = configured.mapNotNull { id -> lookup(id) ?: context.invalid(key, id, expected) }
        if (resolved.isEmpty()) {
            val reason = if (configured.isEmpty()) {
                "no ${names.plural} - a breeder would never breed anything"
            } else {
                "every configured ${names.singular} is invalid"
            }
            context.violation(key, reason)
        }
        return resolved.takeIf { it.isNotEmpty() }
    }

    private class Names(val plural: String, val singular: String)

    private companion object {
        const val DEFAULT_MAX_MOBS = "8"
        val MOBS = Names("mobs", "mob")
        val FOOD = Names("food", "food")
    }
}
