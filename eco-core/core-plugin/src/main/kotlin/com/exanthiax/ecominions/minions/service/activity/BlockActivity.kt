package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionMineEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionMineBlock
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionBlockIndex
import com.exanthiax.ecominions.minions.service.MinionDropTables
import com.exanthiax.ecominions.minions.service.MiningTargetFinder
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviour
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviours
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropResult
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/** Mines, regenerates, replants and fells: the three block types. */
class BlockActivity(
    private val targets: MiningTargetFinder,
    private val blocks: MinionBlockIndex,
    private val regen: RegenService,
    private val behaviours: MinionBehaviours,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher,
    private val dropTables: MinionDropTables,
    private val spawnOrb: (Minion, Int) -> Unit
) : MinionActivity {

    override fun onTick(minion: Minion, now: Long) {
        regen.process(minion, now)
        completePendingBreak(minion, now)
    }

    override fun act(minion: Minion, now: Long): ActivityResult {
        val ready = if (minion.mining.seededLastCycle) targets.find(minion) else null
        if (ready == null && behaviourOf(minion).onBeforeMine(minion, now)) {
            return ActivityResult.SEEDED
        }

        val target = ready ?: targets.find(minion)
            ?: return if (behaviourOf(minion).onIdle(minion, now)) ActivityResult.BUSY else ActivityResult.BLOCKED

        minion.mining.pendingTarget = target
        minion.mining.breakAt = now + minion.stats.efficiencyTicks
        return ActivityResult.WORKED
    }

    override fun hasWork(minion: Minion): Boolean =
        targets.find(minion) != null || behaviourOf(minion).canStock(minion)

    override fun offlineCycle(minion: Minion): OfflineYield? {
        val definition = minion.state.definition
        val block = definition.targetBlocks.definitions.randomOrNull() ?: return null
        return OfflineYield(dropTables.dropsFor(definition, block, minion.location), 0)
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

    /** The minion's own behaviour, for when it has no block in hand to override with. */
    private fun behaviourOf(minion: Minion): MinionBehaviour = behaviours[minion.state.definition.type]

    private fun breakBlock(minion: Minion, block: Block, now: Long) {
        val original = block.type
        val definition = minion.state.definition.targetBlocks.definitionFor(block) ?: blocks.forBlock(block)
        val behaviour = behaviours.forBlock(minion.state.definition.type, definition)
        val column = if (definition?.stackedColumn == true) collectColumn(block) else listOf(block)
        val configured = definition?.let { dropTables.configuredDrops(minion.state.definition, it) }
        val rolled = configured?.let { drops -> column.flatMap { drops.map { it.clone() } } }
            ?: column.flatMap { segment -> definition?.block?.getDrops(segment, null) ?: segment.getDrops() }
        val drops = mineEvent(minion, block, rolled) ?: return

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
                spawnOrb(minion, xp)
            }
        }

        dispatch.storeAndDispatchCollect(minion, results.map { (item, _) -> item })
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
}
