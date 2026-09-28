package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionFuelEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Fuelling a minion from a player's inventory. */
class MinionFuelService(
    private val fuels: FuelService,
    private val repository: MinionStateRepository,
    private val triggers: MinionTriggerDispatch,
    private val events: EventDispatcher
) {
    private companion object {
        const val MILLIS_PER_TICK = 50L
    }

    /**
     * What a single fuelling did.
     *
     * Carries the count as well as the fuel because one deposit can be a whole
     * stack, and the message the player gets says how much went in.
     */
    data class Application(
        val fuel: Fuel,
        val count: Int
    )

    sealed interface Outcome {
        data class Applied(val application: Application) : Outcome
        data object NoFuel : Outcome
        data object Cancelled : Outcome
    }

    /**
     * Takes up to [limit] fuel items from [player] and burns them on [minion].
     *
     * Only one fuel type goes in per deposit: fuels do not stack across types,
     * so draining a mixed inventory into the minion would throw most of it
     * away. The type is the one already burning if the player is carrying more
     * of it, otherwise the first fuel found.
     *
     * The event fires before any item is taken, so cancelling it leaves the
     * player's inventory alone.
     */
    fun fuelFrom(player: Player, minion: Minion, limit: Int = 1): Outcome {
        val preview = player.inventory.contents.map { it?.clone() }.toTypedArray()
        val planned = take(preview, minion, limit) ?: return Outcome.NoFuel
        val duration = authorise(minion, player, planned) ?: return Outcome.Cancelled

        val contents = player.inventory.contents
        take(contents, minion, limit)
        player.inventory.contents = contents
        start(minion, planned.fuel, duration)
        return Outcome.Applied(planned)
    }

    /**
     * Removes up to [limit] items of one fuel from [contents], in place.
     *
     * Split out of [fuelFrom] so the choosing and counting can be tested on
     * their own.
     *
     * @return What was taken, or null if [contents] holds no fuel.
     */
    internal fun take(contents: Array<ItemStack?>, minion: Minion, limit: Int): Application? {
        val fuel = chooseFuel(contents, minion) ?: return null

        var taken = 0
        for (slot in contents.indices) {
            if (taken >= limit) {
                break
            }
            val stack = contents[slot] ?: continue
            if (fuels.matching(stack)?.id != fuel.id) {
                continue
            }

            val moved = minOf(stack.amount, limit - taken)
            stack.amount -= moved
            if (stack.amount <= 0) {
                contents[slot] = null
            }
            taken += moved
        }

        return if (taken > 0) Application(fuel, taken) else null
    }

    /**
     * Burns up to [count] of [item] on [minion], if [item] is a fuel.
     *
     * Separate from [fuelFrom] because an item dropped onto the fuel slot is
     * held on the cursor rather than in a slot of the inventory, and the caller
     * is the one that has to shrink it.
     *
     * @return [Outcome.NoFuel] if [item] is not a fuel.
     */
    fun fuelFromItem(player: Player, minion: Minion, item: ItemStack, count: Int): Outcome {
        if (count <= 0) {
            return Outcome.NoFuel
        }
        val fuel = fuels.matching(item) ?: return Outcome.NoFuel
        val application = Application(fuel, minOf(count, item.amount))
        val duration = authorise(minion, player, application) ?: return Outcome.Cancelled

        start(minion, fuel, duration)
        return Outcome.Applied(application)
    }

    /**
     * Burns [fuel] on [minion] for [durationMillis] with no item involved.
     *
     * @return Whether the event allowed it.
     */
    fun applyDirect(minion: Minion, fuel: Fuel, durationMillis: Long): Boolean {
        val event = MinionFuelEvent(minion, null, fuel, durationMillis)
        if (!events.dispatch(event)) {
            return false
        }
        start(minion, fuel, event.durationMillis)
        return true
    }

    private fun start(minion: Minion, fuel: Fuel, durationMillis: Long) {
        applyMillis(minion, fuel, durationMillis)
        triggers.fuelStarted(minion, fuel.id)
    }

    /** Puts out whatever [minion] is burning. */
    fun clear(minion: Minion) {
        minion.burningFuelId = null
        minion.burningFuelEndsAt = 0L
        minion.burningFuelRemainingMillis = 0L
        repository.save(minion)
    }

    /** @return How long one unit of [fuel] burns for. */
    fun unitMillis(fuel: Fuel): Long = fuel.durationTicks * MILLIS_PER_TICK

    /**
     * @return The fuel a deposit should take, preferring the burning one so a
     *         player topping up a minion extends the burn rather than
     *         replacing it.
     */
    private fun chooseFuel(contents: Array<ItemStack?>, minion: Minion): Fuel? {
        var first: Fuel? = null
        for (stack in contents) {
            val fuel = stack?.let { fuels.matching(it) } ?: continue
            if (fuel.id == minion.burningFuelId) {
                return fuel
            }
            if (first == null) {
                first = fuel
            }
        }
        return first
    }

    /**
     * Fires once per deposit rather than once per item: a stack of coal is one
     * thing the player did.
     *
     * @return How long the burn should last, or null if it was cancelled.
     */
    private fun authorise(minion: Minion, player: Player, application: Application): Long? {
        val event = MinionFuelEvent(minion, player, application.fuel, unitMillis(application.fuel) * application.count)
        return if (events.dispatch(event)) event.durationMillis else null
    }

    /**
     * Burns [count] of [fuel] on [minion], adding to whatever is already
     * burning.
     *
     * More of the same fuel extends the burn rather than restarting it, so
     * topping a minion up never throws away time the player already paid for. A
     * different fuel replaces the burn instead: only one speed multiplier can be
     * live at a time, and the remaining time belongs to the fuel that set it.
     */
    fun apply(minion: Minion, fuel: Fuel, count: Int = 1, now: Long = System.currentTimeMillis()) {
        applyMillis(minion, fuel, unitMillis(fuel) * count, now)
    }

    /** As [apply], for a burn measured in milliseconds rather than items. */
    fun applyMillis(minion: Minion, fuel: Fuel, duration: Long, now: Long = System.currentTimeMillis()) {
        val carried = if (minion.burningFuelId == fuel.id) remainingOf(minion, now) else 0L

        minion.burningFuelId = fuel.id
        minion.burningFuelRemainingMillis = carried + duration
        // An idle minion holds its fuel frozen as a remainder; giving it an
        // absolute expiry here would burn the fuel off while it is not working.
        minion.burningFuelEndsAt = if (minion.state.shutdownTimestamp != null) {
            0L
        } else {
            now + carried + duration
        }
        repository.save(minion)
    }

    /** @return What is left of the current burn, live or frozen. */
    private fun remainingOf(minion: Minion, now: Long): Long =
        if (minion.burningFuelEndsAt > 0L) {
            (minion.burningFuelEndsAt - now).coerceAtLeast(0L)
        } else {
            minion.burningFuelRemainingMillis.coerceAtLeast(0L)
        }
}
