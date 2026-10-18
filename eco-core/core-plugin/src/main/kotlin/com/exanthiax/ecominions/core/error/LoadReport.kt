package com.exanthiax.ecominions.core.error

import java.util.concurrent.atomic.AtomicInteger

/**
 * Counts config violations across every loader so a single startup summary can
 * report them. Loaders never print the summary themselves.
 */
class LoadReport {
    private val violations = AtomicInteger(0)

    /**
     * True during eco's preliminary reload, which runs before other plugins have
     * registered their custom blocks and items, so its violations are not real.
     */
    @Volatile
    var quiet: Boolean = false
    private val loaded = LinkedHashMap<String, Int>()

    val violationCount: Int
        get() = violations.get()

    fun recordViolation() {
        violations.incrementAndGet()
    }

    fun recordLoaded(category: String, count: Int) {
        loaded[category] = count
    }

    fun reset() {
        violations.set(0)
        loaded.clear()
    }

    /** One line, e.g. `Loaded 15 minion types, 10 upgrades (2 violations - see above)`. */
    fun summary(): String {
        val counts = loaded.entries.joinToString(", ") { "${it.value} ${it.key}" }
        val body = if (counts.isBlank()) "Loaded nothing" else "Loaded $counts"
        val count = violationCount
        return when (count) {
            0 -> body
            1 -> "$body (1 violation - see above)"
            else -> "$body ($count violations - see above)"
        }
    }
}
