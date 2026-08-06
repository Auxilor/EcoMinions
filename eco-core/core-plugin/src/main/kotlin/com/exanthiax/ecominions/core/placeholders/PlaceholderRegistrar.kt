package com.exanthiax.ecominions.core.placeholders

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.RegistrablePlaceholder

/**
 * Single owner of the plugin's entire placeholder surface. Grep here to see every
 * placeholder the plugin exposes or expands, in either scope.
 */
class PlaceholderRegistrar(
    private val plugin: EcoPlugin
) {
    private val internal = mutableMapOf<String, InternalPlaceholder>()

    fun registerAll(sources: List<PlaceholderSource>) {
        internal.clear()

        for (source in sources) {
            for (placeholder in source.externalPlaceholders(plugin)) {
                (placeholder as? RegistrablePlaceholder)?.register()
            }
            for (placeholder in source.internalPlaceholders()) {
                internal[placeholder.key] = placeholder
            }
        }
    }

    /** @return The resolved value of `%[key]%`, or null if nothing declares it. */
    fun resolveInternal(key: String, context: TemplateContext): String? =
        internal[key]?.resolve?.invoke(context)
}
