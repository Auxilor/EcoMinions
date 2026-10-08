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
    @Volatile
    private var internal = emptyMap<String, InternalPlaceholder>()

    @Volatile
    private var dynamic = emptyList<DynamicInternalPlaceholder>()

    /**
     * Builds the new set aside and swaps it in whole, because placeholders are
     * resolved on every region at once under Folia.
     */
    fun registerAll(sources: List<PlaceholderSource>) {
        val internal = mutableMapOf<String, InternalPlaceholder>()
        val dynamic = mutableListOf<DynamicInternalPlaceholder>()

        for (source in sources) {
            for (placeholder in source.externalPlaceholders(plugin)) {
                (placeholder as? RegistrablePlaceholder)?.register()
            }
            for (placeholder in source.internalPlaceholders()) {
                internal[placeholder.key] = placeholder
            }
            dynamic += source.dynamicPlaceholders()
        }

        this.internal = internal
        this.dynamic = dynamic
    }

    /** @return The resolved value of `%[key]%`, or null if nothing declares it. */
    fun resolveInternal(key: String, context: TemplateContext): String? =
        internal[key]?.resolve?.invoke(context)
            ?: dynamic.firstNotNullOfOrNull { it.resolve(key, context) }
}
