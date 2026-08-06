package com.exanthiax.ecominions.core.placeholders

/**
 * The values an internal placeholder may resolve against, e.g. the minion whose
 * menu is being rendered.
 */
class TemplateContext(private val values: Map<String, Any?>) {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? = values[key] as? T

    fun raw(key: String): Any? = values[key]

    fun plus(vararg pairs: Pair<String, Any?>): TemplateContext =
        TemplateContext(values + pairs)

    companion object {
        val EMPTY = TemplateContext(emptyMap())

        fun of(vararg pairs: Pair<String, Any?>): TemplateContext = TemplateContext(pairs.toMap())
    }
}
