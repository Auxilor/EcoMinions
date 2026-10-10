package com.exanthiax.ecominions.core.config

/** Reads the `3` or `2-5` form used by `xp:` and `amount:`. */
object IntRanges {
    fun parse(raw: String?): IntRange? {
        val parts = raw?.trim()?.takeIf { it.isNotEmpty() }?.split("-") ?: return null
        val bounds = parts.map { it.trim().toIntOrNull() ?: return null }
        return when (bounds.size) {
            1 -> bounds[0].takeIf { it >= 0 }?.let { it..it }
            2 -> (bounds[0]..bounds[1]).takeIf { bounds[0] >= 0 && bounds[0] <= bounds[1] }
            else -> null
        }
    }
}
