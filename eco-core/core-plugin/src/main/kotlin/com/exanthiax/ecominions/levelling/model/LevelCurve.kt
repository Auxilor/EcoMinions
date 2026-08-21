package com.exanthiax.ecominions.levelling.model

/**
 * How much XP a minion needs to reach a given level.
 *
 * Minions start at level 1, not 0, so the first requirement is the cost of
 * reaching level 2. Pure: the formula evaluator is injected, which is what lets
 * curve arithmetic be tested without an expression engine.
 */
sealed interface LevelCurve {

    /** The highest level this curve can reach. */
    val maxLevel: Int

    /** @return XP needed to go from [level] - 1 to [level]. */
    fun xpToReach(level: Int): Double

    /** An explicit XP cost per level. Running off the end of the list ends progression. */
    class Requirements(
        private val requirements: List<Double>,
        configuredMaxLevel: Int?
    ) : LevelCurve {
        override val maxLevel: Int = configuredMaxLevel ?: (requirements.size + 1)

        override fun xpToReach(level: Int): Double =
            requirements.getOrNull(level - 2) ?: Double.POSITIVE_INFINITY
    }

    /** An expression in `%level%`, evaluated for the level being reached. */
    class Formula(
        private val expression: String,
        configuredMaxLevel: Int?,
        private val evaluate: (String, Int) -> Double
    ) : LevelCurve {
        override val maxLevel: Int = configuredMaxLevel ?: Int.MAX_VALUE

        override fun xpToReach(level: Int): Double = evaluate(expression, level)
    }
}
