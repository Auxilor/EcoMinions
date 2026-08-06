package com.exanthiax.ecominions.core.placeholders

import com.willfp.eco.util.StringUtils

/**
 * The only place placeholder expansion happens. Slices hand it configured text
 * plus a context; it substitutes `%key%` tokens and applies colour formatting.
 *
 * A token is resolved from the context first, then from the registrar's INTERNAL
 * scope, and is otherwise left untouched so unknown tokens stay visible.
 */
class TemplateRenderer(
    private val registrar: PlaceholderRegistrar
) {
    fun render(text: String, context: TemplateContext = TemplateContext.EMPTY): String =
        StringUtils.format(expand(text, context))

    fun render(lines: List<String>, context: TemplateContext = TemplateContext.EMPTY): List<String> =
        lines.map { render(it, context) }

    private fun expand(text: String, context: TemplateContext): String =
        TOKEN.replace(text) { match ->
            val key = match.groupValues[1]
            val fromContext = context.raw(key)?.toString()
            fromContext ?: registrar.resolveInternal(key, context) ?: match.value
        }

    private companion object {
        val TOKEN = Regex("%([a-zA-Z0-9_]+)%")
    }
}
