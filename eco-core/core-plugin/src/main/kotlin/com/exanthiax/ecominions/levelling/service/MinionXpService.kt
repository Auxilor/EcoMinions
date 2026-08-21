package com.exanthiax.ecominions.levelling.service

import com.exanthiax.ecominions.api.event.MinionLevelUpEvent
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.impl.TriggerBlank
import org.bukkit.Bukkit

/**
 * Accumulates minion XP and runs the level-up flow.
 *
 * The arithmetic lives in [progress], which is pure, so the level ladder can be
 * tested without a server. [grant] is the side-effecting half.
 */
class MinionXpService(
    private val applyState: (Minion, MinionState) -> Unit,
    private val onLevelChanged: (Minion) -> Unit
) {
    /** @param levelsGained The levels crossed, or null if none were. */
    data class LevelUpResult(
        val newState: MinionState,
        val levelsGained: IntRange?
    )

    /** Sums every gain method that applies to [dispatched] and banks the total. */
    fun onTrigger(minion: Minion, dispatched: DispatchedTrigger) {
        val levelling = minion.state.type.levelling
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
    fun grant(minion: Minion, amount: Double) {
        val result = progress(minion.state, amount)

        applyState(minion, result.newState)
        onLevelChanged(minion)

        val gained = result.levelsGained ?: return

        for (level in gained) {
            Bukkit.getPluginManager().callEvent(
                MinionLevelUpEvent(minion, level - 1, level)
            )

            val chain = minion.state.type.levelling.levelUpEffects ?: continue
            val dispatcher = minion.toDispatcher()

            val dispatched = DispatchedTrigger(
                dispatcher,
                TriggerBlank,
                TriggerData(
                    dispatcher = dispatcher,
                    player = minion.ownerPlayer(),
                    location = minion.location,
                    value = level.toDouble()
                )
            )
            dispatched.addPlaceholder(NamedValue("level", level))
            dispatched.addPlaceholder(NamedValue("previous_level", level - 1))

            chain.trigger(dispatched)
        }
    }

    companion object {
        /**
         * Pure XP arithmetic.
         *
         * The loop is bounded by a non-positive or non-finite requirement as
         * well as by max level: a curve that costs nothing must not hang the
         * server tick.
         */
        fun progress(state: MinionState, amount: Double): LevelUpResult {
            val levelling = state.type.levelling
            val curve = levelling.curve

            if (!levelling.enabled || curve == null || state.isAtMaxLevel) {
                return LevelUpResult(state, null)
            }

            var level = state.level
            var xp = state.xp + amount
            val firstGained = level + 1

            while (level < levelling.maxLevel) {
                val required = curve.xpToReach(level + 1)
                if (required <= 0.0 || !required.isFinite() || xp < required) {
                    break
                }
                xp -= required
                level++
            }

            if (level == state.level) {
                return LevelUpResult(state.withXp(xp, level), null)
            }

            if (level >= levelling.maxLevel) {
                xp = 0.0
            }

            return LevelUpResult(state.withXp(xp, level), firstGained..level)
        }
    }
}
