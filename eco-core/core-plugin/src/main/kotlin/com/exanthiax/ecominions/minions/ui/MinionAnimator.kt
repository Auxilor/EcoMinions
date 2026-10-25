package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionModels
import org.bukkit.util.EulerAngle

/** Poses a minion's stand, or animates its custom model, to match what it is doing. */
class MinionAnimator(private val models: MinionModels) {

    fun apply(minion: Minion, now: Long) {
        val stand = minion.entity
        if (stand.isDead) {
            return
        }

        if (models.animate(minion, minion.animation)) {
            return
        }

        when (minion.animation) {
            Minion.AnimationState.IDLE, Minion.AnimationState.BLOCKED -> {
                stand.leftArmPose = REST
                stand.rightArmPose = REST
                stand.headPose = EulerAngle(bobOf(now), 0.0, 0.0)
            }

            Minion.AnimationState.MINING -> {
                val raised = (now / SWING_PERIOD_TICKS) % 2L == 0L
                stand.rightArmPose = EulerAngle(
                    Math.toRadians(if (raised) SWING_HIGH else SWING_LOW),
                    0.0,
                    0.0
                )
            }

            Minion.AnimationState.FULL -> {
                stand.leftArmPose = EulerAngle(Math.toRadians(ARMS_UP), 0.0, Math.toRadians(ARMS_OUT))
                stand.rightArmPose = EulerAngle(Math.toRadians(ARMS_UP), 0.0, Math.toRadians(-ARMS_OUT))
            }
        }
    }

    /** An idle minion's stand stays still, but its model loops its idle animation. */
    fun rest(minion: Minion) {
        models.animate(minion, Minion.AnimationState.IDLE)
    }

    /** A four-step bob so an idle minion does not look frozen. */
    private fun bobOf(now: Long): Double = when ((now / BOB_PERIOD_TICKS) % 4L) {
        0L -> 0.0
        1L -> BOB_AMPLITUDE
        2L -> 0.0
        else -> -BOB_AMPLITUDE
    }

    private companion object {
        val REST = EulerAngle(0.0, 0.0, 0.0)

        const val BOB_PERIOD_TICKS = 5L
        const val BOB_AMPLITUDE = 0.05
        const val SWING_PERIOD_TICKS = 2L
        const val SWING_HIGH = -90.0
        const val SWING_LOW = -45.0
        const val ARMS_UP = -100.0
        const val ARMS_OUT = 20.0
    }
}
