package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionFuelExpireEvent
import com.exanthiax.ecominions.api.event.MinionStorageFullEvent
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.libreforge.triggers.TriggerGroupMinionStatic
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFuelExpire
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionStorageFull
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionTick
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.service.activity.ActivityResult
import com.exanthiax.ecominions.minions.service.activity.MinionActivity
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.updateEffects

/**
 * Drives one minion for a single tick.
 *
 * Holds no state of its own: everything that varies per minion lives in that
 * minion's [MiningState], so one instance serves every minion on the server.
 */
class MinionTicker(
    private val worlds: WorldSettings,
    private val fuels: FuelService,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher,
    private val activityOf: (MinionType) -> MinionActivity,
    /** A lambda, not a reference: libreforge is not on the classpath yet when the plugin is constructed. */
    private val dueTriggers: (Long) -> List<Trigger> = { TriggerGroupMinionStatic.dueOn(it) },
    private val updateConditions: (Minion) -> Unit = { it.toDispatcher().updateEffects() },
    /** Feeders and breeders keep working with full storage: it holds their input, not their output. */
    private val takesInput: (MinionDefinition) -> Boolean = { false }
) {
    fun tick(minion: Minion, now: Long) {
        if (minion.entity.isDead || worlds.isDisabled(minion.entity.world)) {
            return
        }

        checkFuelExpiry(minion)
        pollConditions(minion, now)

        val activity = activityOf(minion.state.definition.type)
        activity.onTick(minion, now)
        dispatch.trigger(minion, TriggerMinionTick)
        for (trigger in dueTriggers(now)) {
            dispatch.trigger(minion, trigger)
        }

        if (now < minion.mining.nextActionAt) {
            return
        }

        if (minion.isStorageFull && !takesInput(minion.state.definition)) {
            minion.animation = Minion.AnimationState.FULL
            if (!minion.mining.wasFull) {
                minion.mining.wasFull = true
                events.dispatch(MinionStorageFullEvent(minion))
            }
            dispatch.trigger(minion, TriggerMinionStorageFull)
            minion.mining.nextActionAt = now + FULL_RETRY_TICKS
            return
        }
        minion.mining.wasFull = false

        minion.blockedReason = Minion.NO_TARGET
        apply(minion, activity.act(minion, now), now)
    }

    fun hasWork(minion: Minion): Boolean = activityOf(minion.state.definition.type).hasWork(minion)

    private fun apply(minion: Minion, result: ActivityResult, now: Long) {
        val speed = fuels.effectiveSpeedTicks(minion, minion.stats.speedTicks)
        minion.mining.seededLastCycle = result == ActivityResult.SEEDED
        when (result) {
            ActivityResult.WORKED -> {
                minion.animation = Minion.AnimationState.MINING
                minion.mining.nextActionAt = now + speed + minion.stats.efficiencyTicks
            }
            ActivityResult.SEEDED, ActivityResult.BUSY -> {
                minion.animation = Minion.AnimationState.IDLE
                minion.mining.nextActionAt = now + speed
            }
            ActivityResult.BLOCKED -> {
                minion.animation = Minion.AnimationState.BLOCKED
                minion.mining.nextActionAt = now + speed
            }
        }
    }

    /**
     * Fuel expiry is never announced - it is inferred from the clock - so the
     * transition has to be spotted by comparing against the previous tick.
     */
    private fun checkFuelExpiry(minion: Minion) {
        val burning = fuels.activeFuelOf(minion) != null

        if (minion.mining.wasBurningFuel && !burning) {
            dispatch.trigger(minion, TriggerMinionFuelExpire)
            minion.burningFuelId?.let { fuels.getById(it) }?.let { events.dispatch(MinionFuelExpireEvent(minion, it)) }
        }

        minion.mining.wasBurningFuel = burning
    }

    /**
     * libreforge never polls a custom dispatcher, so conditions that read
     * minion state (fuel, upgrades, storage, level) are re-evaluated here on
     * the same interval libreforge uses for players.
     */
    private fun pollConditions(minion: Minion, now: Long) {
        if (now - minion.mining.conditionsPolledAt < CONDITION_POLL_TICKS) {
            return
        }
        minion.mining.conditionsPolledAt = now
        updateConditions(minion)
    }

    private companion object {
        const val FULL_RETRY_TICKS = 20L
        const val CONDITION_POLL_TICKS = 20L
    }
}
