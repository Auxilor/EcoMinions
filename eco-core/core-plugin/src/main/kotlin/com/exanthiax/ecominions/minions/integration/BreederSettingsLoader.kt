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
    fun load(config: Config, context: LoadContext): BreederSettings? {
        val targets = resolve(config, context, MOBS, entities::lookup) ?: return null
        return resolve(config, context, FOOD, items::lookup)?.let { food ->
            BreederSettings(targets = targets, food = food, maxMobs = maxMobsOf(config))
        }
    }

    private fun maxMobsOf(config: Config): String =
        config.getStringOrNull("max-mobs")?.takeIf { it.isNotBlank() } ?: DEFAULT_MAX_MOBS

    private fun <T : Any> resolve(
        config: Config,
        context: LoadContext,
        field: Field,
        lookup: (String) -> T?
    ): List<T>? {
        val configured = config.getStrings(field.key).filter { it.isNotBlank() }
        val resolved = configured.mapNotNull { id ->
            lookup(id) ?: context.invalid(param = field.key, value = id, expected = field.expected)
        }
        if (resolved.isEmpty()) {
            val reason = if (configured.isEmpty()) {
                "no ${field.plural} - a breeder would never breed anything"
            } else {
                "every configured ${field.singular} is invalid"
            }
            context.violation(param = field.key, message = reason)
        }
        return resolved.takeIf { it.isNotEmpty() }
    }

    private class Field(val key: String, val expected: String, val plural: String, val singular: String)

    private companion object {
        const val DEFAULT_MAX_MOBS = "8"
        val MOBS = Field(key = "target-mobs", expected = "a mob or custom mob id", plural = "mobs", singular = "mob")
        val FOOD = Field(key = "food", expected = "an item or custom item id", plural = "food", singular = "food")
    }
}
