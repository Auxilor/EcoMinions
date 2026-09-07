package com.exanthiax.ecominions.core.placeholders

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.Placeholder

/** A placeholder resolved inside lore, messages and holograms, e.g. `%level%`. */
class InternalPlaceholder(
    val key: String,
    val resolve: (TemplateContext) -> String?
)

/**
 * A placeholder whose key is a shape rather than a fixed string, e.g. `%level_2%`.
 *
 * Exists because a relative placeholder has unbounded keys - `%level_1%`, `%level_-3%` - so it
 * cannot be a map entry. Consulted only after every fixed key has missed, so it can never
 * shadow a declared placeholder.
 */
class DynamicInternalPlaceholder(
    val resolve: (key: String, context: TemplateContext) -> String?
)

/**
 * A slice's contribution to the plugin's placeholder surface, in both scopes:
 * EXTERNAL (registered with eco/PlaceholderAPI) and INTERNAL (expanded in text
 * by [TemplateRenderer]). Sources are registered by the composition root; they
 * never register themselves.
 */
interface PlaceholderSource {
    fun externalPlaceholders(plugin: EcoPlugin): List<Placeholder> = emptyList()

    fun internalPlaceholders(): List<InternalPlaceholder> = emptyList()

    fun dynamicPlaceholders(): List<DynamicInternalPlaceholder> = emptyList()
}
