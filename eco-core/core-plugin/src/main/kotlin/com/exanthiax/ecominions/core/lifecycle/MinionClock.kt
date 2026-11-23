package com.exanthiax.ecominions.core.lifecycle

/**
 * Server ticks, counted by the mining loop on the global region. Spigot has no server tick
 * counter to read, and the count survives reloads so stored break times stay valid.
 */
object MinionClock {
    @Volatile
    var now: Long = 0L
        private set

    fun advance(): Long {
        now += 1
        return now
    }
}
