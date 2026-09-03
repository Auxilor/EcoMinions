package com.exanthiax.ecominions.minions.service

import kotlin.math.roundToInt

/** Works out which way a freshly placed minion should look. */
object MinionFacing {
    private const val QUARTER_TURN = 90.0f
    private const val FULL_TURN = 360.0f

    /**
     * The yaw a minion needs to look back at a player standing at [playerYaw],
     * squared off to the nearest cardinal direction.
     */
    fun towards(playerYaw: Float): Float {
        val facing = playerYaw + (FULL_TURN / 2)
        val squared = (facing / QUARTER_TURN).roundToInt() * QUARTER_TURN
        return ((squared % FULL_TURN) + FULL_TURN) % FULL_TURN
    }
}
