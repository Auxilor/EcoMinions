package com.exanthiax.ecominions.core.error

import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.ViolationContext

/**
 * Handed to a loader for one config file. Every rejected value goes through here:
 * skipping without reporting is not permitted, because a typo that silently
 * deletes behaviour is indistinguishable from working config.
 */
class LoadContext(
    private val violations: ViolationContext,
    private val report: LoadReport
) {
    /** Reports a problem with [param] and counts it towards the startup summary. */
    fun violation(param: String, message: String) {
        report.recordViolation()
        violations.log(ConfigViolation(param, message))
    }

    /**
     * Reports an unresolvable id, listing what would have worked.
     *
     * @return null, so callers can `return context.unknown(...)`.
     */
    fun <T> unknown(param: String, value: String, known: Collection<String>): T? {
        val alternatives = known.sorted().joinToString(", ")
        violation(param, "'$value' not found - known: [$alternatives]")
        return null
    }

    /**
     * Reports a value that is malformed rather than merely unrecognised, where
     * listing every alternative would be noise (`'GRANIT' is not a material`).
     *
     * @return null, so callers can `return context.invalid(...)`.
     */
    fun <T> invalid(param: String, value: String, expected: String): T? {
        violation(param, "'$value' is not $expected")
        return null
    }

    /** @return [value], or null after reporting [message] if it was null. */
    fun <T : Any> required(value: T?, param: String, message: String): T? {
        if (value == null) {
            violation(param, message)
        }
        return value
    }

    /** @return A context scoped to a nested config section, for clearer paths. */
    fun with(section: String): LoadContext = LoadContext(violations.with(section), report)
}
