package com.exanthiax.ecominions.core.placeholders

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.Placeholder

/** A placeholder resolved inside lore, messages and holograms, e.g. `%level%`. */
class InternalPlaceholder(
    val key: String,
    val resolve: (TemplateContext) -> String?
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
}
