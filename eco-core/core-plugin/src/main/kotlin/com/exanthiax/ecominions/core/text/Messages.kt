package com.exanthiax.ecominions.core.text

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.placeholders.TemplateRenderer
import com.willfp.eco.core.config.interfaces.Config

/**
 * Reads admin-authored text out of `lang.yml` and renders it.
 *
 * The only route from a lang path to a finished string; edges never format text
 * themselves.
 */
class Messages(
    private val lang: Config,
    private val renderer: TemplateRenderer
) {
    fun line(path: String, context: TemplateContext = TemplateContext.EMPTY): String =
        renderer.render(lang.getString(path), context)

    fun lines(path: String, context: TemplateContext = TemplateContext.EMPTY): List<String> =
        renderer.render(lang.getStrings(path), context)

    /** Renders text that came from somewhere other than `lang.yml`, e.g. a menu title. */
    fun render(text: String, context: TemplateContext = TemplateContext.EMPTY): String =
        renderer.render(text, context)

    /** Unrendered text, for callers that need the raw template. */
    fun raw(path: String): String = lang.getString(path)

    /**
     * Renders [path] and drops each line whose template uses [placeholder] when that
     * placeholder rendered to nothing, so an empty optional value leaves no blank line.
     */
    fun linesWithout(path: String, placeholder: String, context: TemplateContext): List<String> {
        val templates = lang.getStrings(path)
        return renderer.render(templates, context).filterIndexed { index, line ->
            line.isNotBlank() || templates.getOrNull(index)?.contains("%$placeholder%") != true
        }
    }
}
