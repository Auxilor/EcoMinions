package com.exanthiax.ecominions.minions.model

/**
 * How much a minion is credited with mining while it was idle.
 *
 * A minion is idle whenever its owner is offline or its chunk is unloaded. The
 * window is walked one cycle at a time rather than divided, because fuel is
 * consumed by the walk and a fuelled cycle is shorter than an unfuelled one.
 */
object OfflineProgress {

    private const val MILLIS_PER_TICK = 50L

    /**
     * Ceiling on how many cycles one walk may credit.
     *
     * The walk is O(cycles), so an absurd elapsed time - a clock jump, a world
     * restored from an old backup - must not be allowed to stall the tick that
     * triggered it. A minion's storage fills long before this in practice.
     */
    const val MAX_SIMULATED_CYCLES = 1_000_000

    /**
     * @param cycles How many mine cycles to credit.
     * @param fuelMillisConsumed How much of the remaining fuel the walk burned.
     */
    data class Result(
        val cycles: Int,
        val fuelMillisConsumed: Long
    )

    /**
     * Walks the idle window, calling [onCycle] once per credited cycle.
     *
     * [onCycle] returns false to stop early, which is how a full storage ends the
     * walk without spending the fuel the remaining cycles would have used.
     *
     * @param elapsedMillis Wall-clock time the minion was idle for.
     * @param efficiency Fraction of the live rate credited while idle; 1.0 is parity.
     * @param fuelRemainingMillis Fuel left when the minion went idle; 0 for none.
     * @param fueledCycleTicks Cycle length in ticks while that fuel burns.
     */
    fun simulate(
        elapsedMillis: Long,
        efficiency: Double,
        stats: MinionStats,
        fuelRemainingMillis: Long,
        fueledCycleTicks: Int,
        onCycle: () -> Boolean
    ): Result {
        if (elapsedMillis <= 0 || efficiency <= 0.0) {
            return Result(0, 0L)
        }

        var budget = (elapsedMillis * efficiency).toLong()
        val baseCycleMillis = (stats.cycleTicks * MILLIS_PER_TICK).coerceAtLeast(1L)
        val fueledCycleMillis = (fueledCycleTicks * MILLIS_PER_TICK).coerceAtLeast(1L)

        var cycles = 0
        var fuelUsed = 0L

        while (cycles < MAX_SIMULATED_CYCLES) {
            val fuelLeft = fuelRemainingMillis - fuelUsed
            val cycleMillis = if (fuelLeft > 0) fueledCycleMillis else baseCycleMillis

            if (cycleMillis > budget) {
                break
            }
            if (!onCycle()) {
                break
            }

            budget -= cycleMillis
            cycles++
            if (fuelLeft > 0) {
                fuelUsed += minOf(cycleMillis, fuelLeft)
            }
        }

        return Result(cycles, fuelUsed)
    }
}
