package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionMineEvent
import com.exanthiax.ecominions.api.event.MinionStorageFullEvent
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerGroupMinionStatic
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFuelExpire
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionMineBlock
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionStorageFull
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionTick
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviour
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviours
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropResult
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ExperienceOrb
import org.bukkit.inventory.ItemStack

/**
 * Drives one minion's mine/regen cycle for a single tick.
 *
 * Holds no state of its own: everything that varies per minion lives in that
 * minion's [MiningState], so one instance serves every minion on the server.
 */
class MiningService(
    private val settings: SettingsHolder<MinionSettings>,
    private val worlds: WorldSettings,
    private val targets: MiningTargetFinder,
    private val blocks: MinionBlockIndex,
    private val regen: RegenService,
    private val behaviours: MinionBehaviours,
    private val fuels: FuelService,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher
) {
    fun tick(minion: Minion, now: Long) {
        if (minion.entity.isDead || worlds.isDisabled(minion.entity.world)) {
            return
        }

        checkFuelExpiry(minion)

        regen.process(minion, now)
        completePendingBreak(minion, now)
        dispatch.trigger(minion, TriggerMinionTick)
        for (trigger in TriggerGroupMinionStatic.dueOn(now)) {
            dispatch.trigger(minion, trigger)
        }

        if (now < minion.mining.nextActionAt) {
            return
        }

        if (minion.isStorageFull) {
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

        if (behaviourOf(minion).onBeforeMine(minion, now)) {
            minion.animation = Minion.AnimationState.IDLE
            minion.mining.nextActionAt = now + effectiveSpeedTicks(minion)
            return
        }

        val target = targets.find(minion)
        if (target == null) {
            handleNoTarget(minion, now)
            return
        }

        beginBreak(minion, target, now)
    }

    /**
     * Fuel expiry is never announced - it is inferred from the clock - so the
     * transition has to be spotted by comparing against the previous tick.
     */
    private fun checkFuelExpiry(minion: Minion) {
        val burning = fuels.activeFuelOf(minion) != null

        if (minion.mining.wasBurningFuel && !burning) {
            dispatch.trigger(minion, TriggerMinionFuelExpire)
        }

        minion.mining.wasBurningFuel = burning
    }

    private fun beginBreak(minion: Minion, target: Block, now: Long) {
        val stats = minion.stats
        minion.animation = Minion.AnimationState.MINING
        minion.mining.pendingTarget = target
        minion.mining.breakAt = now + stats.efficiencyTicks
        minion.mining.nextActionAt = now + effectiveSpeedTicks(minion) + stats.efficiencyTicks
    }

    private fun completePendingBreak(minion: Minion, now: Long) {
        val target = minion.mining.pendingTarget ?: return
        if (now < minion.mining.breakAt) {
            return
        }
        minion.mining.pendingTarget = null

        if (!minion.state.definition.targetBlocks.matches(target)) {
            return
        }
        breakBlock(minion, target, now)
    }

    /** Tree types run their own plant-wait-fell cycle; everything else waits on its next seed. */
    private fun handleNoTarget(minion: Minion, now: Long) {
        val busy = behaviourOf(minion).onIdle(minion, now)
        minion.animation = if (busy) Minion.AnimationState.IDLE else Minion.AnimationState.BLOCKED
        minion.mining.nextActionAt = now + effectiveSpeedTicks(minion)
    }

    /** The minion's own behaviour, for when it has no block in hand to override with. */
    private fun behaviourOf(minion: Minion): MinionBehaviour = behaviours[minion.state.definition.type]

    private fun effectiveSpeedTicks(minion: Minion): Int =
        fuels.effectiveSpeedTicks(minion, minion.stats.speedTicks)

    private fun breakBlock(minion: Minion, block: Block, now: Long) {
        val original = block.type
        val definition = blocks.forBlock(block)
        val behaviour = behaviours.forBlock(minion.state.definition.type, definition)
        val column = if (definition?.stackedColumn == true) collectColumn(block) else listOf(block)
        val drops = mineEvent(minion, block, column.flatMap { it.getDrops() }) ?: return

        val results = dispatch.drops(
            minion = minion,
            trigger = TriggerMinionMineBlock,
            drops = drops,
            block = block,
            location = block.location,
            cause = DropCause.BLOCK,
            value = column.size.toDouble()
        )

        storeDrops(minion, results)

        for (segment in column.drop(1)) {
            segment.type = Material.AIR
        }

        behaviour.onBlockBroken(minion, block, definition, original, now)
    }

    /**
     * Skipped outright when nothing listens, because this runs for every block
     * every minion breaks.
     *
     * @return The drops to use, or null if the break was cancelled.
     */
    private fun mineEvent(minion: Minion, block: Block, drops: List<ItemStack>): List<ItemStack>? {
        if (MinionMineEvent.getHandlerList().registeredListeners.isEmpty()) {
            return drops
        }
        val event = MinionMineEvent(minion, block, drops.toMutableList())
        return if (events.dispatch(event)) event.drops else null
    }

    private fun storeDrops(minion: Minion, results: List<DropResult>) {
        for ((_, xp) in results) {
            if (xp > 0) {
                spawnXp(minion, xp)
            }
        }

        dispatch.storeAndDispatchCollect(minion, results.map { (item, _) -> item })
    }

    private fun spawnXp(minion: Minion, amount: Int) {
        val location = minion.entity.location.clone().add(0.0, 0.5, 0.0)
        minion.entity.world.spawn(location, ExperienceOrb::class.java).experience = amount
    }

    private fun collectColumn(bottom: Block): List<Block> {
        val column = mutableListOf(bottom)
        val world = bottom.world
        var y = bottom.y + 1
        while (y < world.maxHeight) {
            val above = world.getBlockAt(bottom.x, y, bottom.z)
            if (above.type != bottom.type) {
                break
            }
            column += above
            y++
        }
        return column
    }

    private companion object {
        const val FULL_RETRY_TICKS = 20L
    }
}
