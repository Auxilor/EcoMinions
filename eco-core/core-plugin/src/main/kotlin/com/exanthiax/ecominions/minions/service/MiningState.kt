package com.exanthiax.ecominions.minions.service

import org.bukkit.Location
import com.willfp.eco.core.blocks.TestableBlock
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData

/** Where a single minion is up to in its mine/regen cycle. Never persisted. */
class MiningState {
    var nextActionAt: Long = 0L

    /** Whether fuel was burning last tick, so expiry can be noticed the tick it happens. */
    var wasBurningFuel: Boolean = false

    /** The block being swung at, and the tick the swing lands on. */
    var pendingTarget: Block? = null
    var breakAt: Long = 0L

    /** Set while a tree minion is waiting for its sapling to mature. */
    var saplingPosition: Location? = null
    var saplingPlantedAt: Long = 0L

    /**
     * The tree a woodcutter is currently cutting down.
     *
     * Its leaves stay standing until its last log is felled, so the tracking has
     * to outlive the tick that grew it.
     */
    var standingTree: TreeService.GrownTree? = null

    val regenQueue: MutableList<RegenEntry> = mutableListOf()

    /** Blocks currently showing the regen marker, cleared if the minion is picked up. */
    val pendingRegen: MutableList<Location> = mutableListOf()

    /**
     * Whether [block] has already been broken and is waiting on its regen timer.
     *
     * Shared, because both halves of the cycle have to leave such a block
     * alone: the finder must not mine the marker standing in for it, and the
     * seeder must not fill the hole ahead of the regen.
     */
    fun isAwaitingRegen(block: Block): Boolean =
        regenQueue.any { entry ->
            entry.location.blockX == block.x &&
                entry.location.blockY == block.y &&
                entry.location.blockZ == block.z
        }

    class RegenEntry(
        val location: Location,
        val regenData: BlockData,
        val surfaceBlock: TestableBlock?,
        val regenAt: Long
    )
}
