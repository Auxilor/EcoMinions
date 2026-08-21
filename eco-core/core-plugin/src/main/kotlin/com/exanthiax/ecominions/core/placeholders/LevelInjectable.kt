package com.exanthiax.ecominions.core.placeholders

import com.willfp.eco.core.placeholder.InjectablePlaceholder
import com.willfp.eco.core.placeholder.PlaceholderInjectable
import com.willfp.eco.core.placeholder.StaticPlaceholder

/**
 * Supplies `%level%` to an eco expression.
 *
 * Separate from [PlaceholderRegistrar] on purpose: that renders plugin text,
 * this feeds eco's expression evaluator, and the two have different lifetimes.
 */
class LevelInjectable(
    level: Int
) : PlaceholderInjectable {
    private val placeholders = listOf(
        StaticPlaceholder("level") { level.toString() }
    )

    override fun getPlaceholderInjections(): List<InjectablePlaceholder> = placeholders

    override fun addInjectablePlaceholder(placeholders: Iterable<InjectablePlaceholder>) = Unit

    override fun clearInjectedPlaceholders() = Unit
}
