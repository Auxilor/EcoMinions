package com.exanthiax.ecominions.minions.model

/** How much a minion is credited with mining while its chunk was unloaded. */
object OfflineProgress {

    private const val MILLIS_PER_TICK = 50L

    /**
     * @param elapsedMillis Wall-clock time the minion was unloaded for.
     * @param efficiency Fraction of the live rate credited offline; 1.0 is parity.
     * @return How many mine cycles to simulate; never negative.
     */
    fun cycles(elapsedMillis: Long, efficiency: Double, stats: MinionStats): Int {
        if (elapsedMillis <= 0 || efficiency <= 0.0) {
            return 0
        }
        val effectiveMillis = (elapsedMillis * efficiency).toLong()
        val cycleMillis = stats.cycleTicks * MILLIS_PER_TICK
        return (effectiveMillis / cycleMillis).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
}
