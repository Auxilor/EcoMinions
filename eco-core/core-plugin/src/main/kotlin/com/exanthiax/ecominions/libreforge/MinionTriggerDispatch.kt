package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionCollect
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFuelStart
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionUpgrade
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.drops.DropContribution
import com.willfp.libreforge.drops.LibreforgeDrops
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropContext
import com.willfp.libreforge.triggers.event.DropResult
import com.willfp.libreforge.triggers.event.EditableDropEvent
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

/** What a drop dispatch left, plus the XP libreforge contributors added. */
data class MinionDrops(val results: List<DropResult>, val contributedExperience: Int)

/**
 * The one path a minion trigger takes.
 *
 * A class rather than the extension functions it replaces, because everything
 * that needs to observe minion triggers - XP gain today - has to be injected,
 * and libreforge's own `TriggerDispatchEvent` is not a usable hook: dispatch
 * returns before firing it when the dispatcher has no triggerable effects and
 * no bound counters, so a type with `xp-gain-methods` but no `effects:` block
 * would silently never gain XP.
 */
class MinionTriggerDispatch(
    private val listener: MinionTriggerListener = MinionTriggerListener.None,
    /** Null calls libreforge directly, which is not on the classpath yet when the plugin is constructed. */
    private val contributions: ((DropCause, DropContext, Location) -> DropContribution)? = null
) {
    /**
     * Runs [drops] through [trigger] as an editable drop event, and returns what
     * the effects left behind.
     *
     * Using libreforge's own drop event is the whole point: every stock drop
     * effect reads it, so autosmelt and multiply_drops work on minions without
     * EcoMinions writing a line of drop-handling logic.
     *
     * Block and entity drops also take libreforge contributions, which is how
     * EcoItems `loots/` reach a minion. Contributed XP rides separately because
     * libreforge's [DropResult] ties XP to one item.
     */
    fun drops(
        minion: Minion,
        trigger: Trigger,
        drops: List<ItemStack>,
        block: Block?,
        location: Location,
        cause: DropCause,
        value: Double = 1.0,
        entity: Entity? = null
    ): MinionDrops {
        val player = minion.ownerPlayer()
        val context = DropContext(player = player, block = block, entity = entity)
        val contributed = contribute(cause, context, location)
        val initial = drops + contributed.items

        val event = EditableDropEvent(
            initialDrops = initial,
            cause = cause,
            context = context,
            dropLocation = location
        ).apply { duplicationSafe = true }

        dispatch(
            minion,
            trigger,
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = player,
                victim = entity as? LivingEntity,
                block = block,
                event = event,
                location = location,
                item = initial.firstOrNull(),
                value = value
            )
        )

        return MinionDrops(event.items, contributed.xp)
    }

    /** Only block and entity drops take contributions; a custom roll is already the whole loot. */
    internal fun contribute(cause: DropCause, context: DropContext, location: Location): DropContribution =
        if (cause == DropCause.BLOCK || cause == DropCause.ENTITY) {
            contributions?.invoke(cause, context, location) ?: LibreforgeDrops.contributions(cause, context, location)
        } else {
            DropContribution.EMPTY
        }

    /** Dispatches [trigger] against [minion] with no drops attached. */
    fun trigger(
        minion: Minion,
        trigger: Trigger,
        block: Block? = null,
        value: Double = 1.0
    ) {
        dispatch(
            minion,
            trigger,
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                block = block,
                location = block?.location ?: minion.location,
                value = value
            )
        )
    }

    /** Dispatches [trigger] against [minion] with [location] as the trigger's location. */
    fun triggerAt(minion: Minion, trigger: Trigger, location: Location, value: Double) {
        dispatch(
            minion,
            trigger,
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                location = location,
                value = value
            )
        )
    }

    /** Dispatches [trigger] against [minion] for one item it just collected. */
    fun collect(minion: Minion, trigger: Trigger, item: ItemStack, value: Double) {
        dispatch(
            minion,
            trigger,
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                item = item,
                location = minion.location,
                value = value
            )
        )
    }

    /**
     * Stores [items] and dispatches [TriggerMinionCollect] for what actually
     * landed in storage.
     *
     * A full minion collects nothing, so it must credit nothing: collections and
     * skills level off this trigger, and crediting a drop that bounced would pay
     * a player for items they never received.
     *
     * @return Everything that did not fit, as [MinionStorage.store] would.
     */
    fun storeAndDispatchCollect(minion: Minion, items: List<ItemStack>): List<ItemStack> {
        val leftovers = mutableListOf<ItemStack>()

        for (item in items) {
            val intended = item.amount
            val remainder = minion.storage.add(item)
            remainder?.let { leftovers += it }

            val stored = intended - (remainder?.amount ?: 0)
            if (stored > 0) {
                collect(minion, TriggerMinionCollect, item, stored.toDouble())
            }
        }

        return leftovers
    }

    /**
     * Dispatches [TriggerMinionFuelStart] for [fuelId].
     *
     * Goes straight to the trigger, as it always has, so XP gain does not see it.
     */
    fun fuelStarted(minion: Minion, fuelId: String) {
        TriggerMinionFuelStart.dispatch(
            minion.toDispatcher(),
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                location = minion.location,
                text = fuelId
            )
        )
    }

    /**
     * Dispatches [TriggerMinionUpgrade] for [tier] of [upgradeId].
     *
     * Goes straight to the trigger, as it always has, so XP gain does not see it.
     */
    fun upgraded(minion: Minion, upgradeId: String, tier: Int) {
        TriggerMinionUpgrade.dispatch(
            minion.toDispatcher(),
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                location = minion.location,
                text = upgradeId,
                value = tier.toDouble()
            )
        )
    }

    private fun dispatch(minion: Minion, trigger: Trigger, data: TriggerData) {
        val dispatcher = minion.toDispatcher()
        trigger.dispatch(dispatcher, data)
        listener.onTrigger(minion, DispatchedTrigger(dispatcher, trigger, data))
    }
}
