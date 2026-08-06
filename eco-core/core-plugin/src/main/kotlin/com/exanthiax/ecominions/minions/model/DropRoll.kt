package com.exanthiax.ecominions.minions.model

/**
 * Turns a fractional drop quantity into a whole number of items.
 *
 * A fortune multiplier of 1.5 on a single drop yields one item plus a 50% chance
 * of a second; the fractional part is the probability of rounding up.
 */
object DropRoll {

    /**
     * @param roll A value in `[0, 1)`, normally random.
     * @return How many items to award.
     */
    fun amount(baseAmount: Int, fortuneMultiplier: Double, roll: Double): Int {
        val quantity = baseAmount * fortuneMultiplier
        if (quantity <= 0.0) {
            return 0
        }
        val whole = quantity.toInt()
        val fraction = quantity - whole
        return whole + if (roll < fraction) 1 else 0
    }
}
