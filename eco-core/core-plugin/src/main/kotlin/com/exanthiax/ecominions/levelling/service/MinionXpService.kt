package com.exanthiax.ecominions.levelling.service

import com.exanthiax.ecominions.api.event.MinionLevelUpEvent
import com.exanthiax.ecominions.api.event.MinionXpGainEvent
import com.exanthiax.ecominions.core.event.BukkitEventDispatcher
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerListener
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionLevelUp
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.progression.LevelProgression
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.levels.LevelUpDispatcher
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.TriggerData

/**
 * Accumulates minion XP and runs the level-up flow.
 *
 * The arithmetic lives in [progress], which is pure, so the level ladder can be
 * tested without a server. [grant] is the side-effecting half.
 */
class MinionXpService(
    private val applyState: (Minion, MinionState) -> Unit,
    private val onLevelChanged: (Minion) -> Unit,
    private val notifyLevelUp: (Minion, Int) -> Unit = { _, _ -> },
    private val events: EventDispatcher = BukkitEventDispatcher
) : MinionTriggerListener {
    /** @param levelsGained The levels crossed, or null if none were. */
    data class LevelUpResult(
        val newState: MinionState,
        val levelsGained: IntRange?
    )

    /** Sums every gain method that applies to [dispatched] and banks the total. */
    override fun onTrigger(minion: Minion, dispatched: DispatchedTrigger) {
        val levelling = minion.state.definition.levelling
        if (!levelling.enabled || minion.state.isAtMaxLevel) {
            return
        }
        if (!levelling.conditions.areMet(minion.toDispatcher(), EmptyProvidedHolder)) {
            return
        }

        var total = 0.0
        for (method in levelling.gainMethods) {
            total += method.amountFor(dispatched) ?: continue
        }

        if (total > 0.0) {
            grant(minion, total)
        }
    }

    /**
     * Banks [amount] and runs the level-up flow for every level it crosses.
     *
     * State and stats are updated before any effect fires, so an effect always
     * observes the finished level. Each crossed level gets its own dispatch, so
     * a reward is not swallowed when a large offline catch-up crosses several
     * levels at once.
     */
    fun grant(minion: Minion, amount: Double): Boolean {
        val levelling = minion.state.definition.levelling
        if (!levelling.enabled || minion.state.isAtMaxLevel) {
            return false
        }
        val granted = allowGain(minion, amount) ?: return false
        val result = progress(minion.state, granted)

        applyState(minion, result.newState)
        onLevelChanged(minion)

        val gained = result.levelsGained ?: return true

        for (level in gained) {
            events.dispatch(MinionLevelUpEvent(minion, level - 1, level))

            val dispatcher = minion.toDispatcher()

            // dispatchTrigger = true keeps this path's global dispatch, which the shared
            // helper leaves off by default. %level_numeral% and %previous_level_numeral% come
            // along with the shared placeholder set; nothing that worked before stops working.
            LevelUpDispatcher.dispatch(
                dispatcher,
                TriggerMinionLevelUp,
                minion.state.definition.levelling.levelUpEffects,
                level,
                TriggerData(
                    dispatcher = dispatcher,
                    player = minion.ownerPlayer(),
                    location = minion.location,
                    value = level.toDouble()
                ),
                dispatchTrigger = true,
                type = "level"
            )

            notifyLevelUp(minion, level)
        }
        return true
    }

    /**
     * Skipped when nothing listens, because gain methods can run on every block
     * a minion breaks.
     *
     * @return The XP to bank, or null if the gain was cancelled or is not positive.
     */
    private fun allowGain(minion: Minion, amount: Double): Double? {
        if (MinionXpGainEvent.getHandlerList().registeredListeners.isEmpty()) {
            return amount.takeIf { it > 0.0 }
        }
        val event = MinionXpGainEvent(minion, amount)
        if (!events.dispatch(event)) {
            return null
        }
        return event.amount.takeIf { it > 0.0 }
    }

    /**
     * Sets [minion]'s level directly with no XP banked and no level-up flow.
     *
     * @return False if its type cannot level or [level] is out of range.
     */
    fun setLevel(minion: Minion, level: Int): Boolean {
        val levelling = minion.state.definition.levelling
        if (!levelling.enabled || level !in 1..levelling.maxLevel) {
            return false
        }
        applyState(minion, minion.state.withXp(0.0, level))
        onLevelChanged(minion)
        return true
    }

    companion object {
        /**
         * Pure XP arithmetic, delegated to eco's shared progression core.
         *
         * `clearXpAtMaxLevel = true` preserves this service's own long-standing
         * policy of zeroing leftover xp on reaching max level, which differs
         * from the shared core's default of banking it. The already-at-max guard
         * above short-circuits before this is ever called for a minion that
         * entered already maxed, so that case still drops the grant entirely
         * rather than banking it, exactly as before.
         */
        fun progress(state: MinionState, amount: Double): LevelUpResult {
            val levelling = state.definition.levelling
            val curve = levelling.curve

            if (!levelling.enabled || curve == null || state.isAtMaxLevel) {
                return LevelUpResult(state, null)
            }

            val change = LevelProgression.progress(
                curve,
                state.level,
                state.xp,
                amount,
                clearXpAtMaxLevel = true
            )

            return LevelUpResult(
                state.withXp(change.newXp, change.newLevel),
                change.levelsGained
            )
        }
    }
}
