package com.exanthiax.ecominions.minions.service

import com.willfp.eco.core.blocks.TestableBlock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.LivingEntity

/** Where a single minion is up to in its mine/regen cycle. Never persisted. */
class MiningState {
    var nextActionAt: Long = 0L

    /** Whether fuel was burning last tick, so expiry can be noticed the tick it happens. */
    var wasBurningFuel: Boolean = false

    /** Whether storage was full at the last action, so filling up is announced once. */
    var wasFull: Boolean = false

    /** The tick this minion's libreforge conditions were last re-evaluated. */
    var conditionsPolledAt: Long = 0L

    /** The block being swung at, and the tick the swing lands on. */
    var pendingTarget: Block? = null
    var breakAt: Long = 0L

    /** Whether the last cycle seeded, so the next one mines instead when it can. */
    var seededLastCycle: Boolean = false

    /** The mob a slayer is swinging at; the kill lands on [breakAt]. */
    var pendingKill: LivingEntity? = null

    /** Live mobs this slayer spawned. Never persisted: recounted from tags when the minion loads. */
    val spawnedMobs: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** The water a fishing minion has cast into; the catch lands on [breakAt]. */
    var pendingCatch: Block? = null

    /** Water sources found by the last scan, and cycles until the next one. */
    var waterBlocks: List<Block> = emptyList()
    var waterScanDue: Int = 0

    /** Which of several seed blocks goes in next, so each is seeded in turn. */
    var nextSeedIndex: Int = 0

    /** A tree minion's saplings, each with the tick it was planted on. */
    val saplings: MutableMap<Location, Long> = mutableMapOf()

    /**
     * The trees a woodcutter has grown and not yet cut down.
     *
     * Each tree's leaves stay standing until its last log is felled, so the
     * tracking has to outlive the tick that grew it.
     */
    val standingTrees: MutableList<TreeService.GrownTree> = mutableListOf()

    val regenQueue: MutableList<RegenEntry> = mutableListOf()

    /**
     * Every block position this minion has placed something at: seeds, saplings,
     * grown trees and the water it laid. Persisted on the stand, so a minion picked
     * up after a restart can still clear what it placed.
     */
    val seeded: MutableSet<Location> = mutableSetOf()

    /** Whether the owner can build at each block position asked about, and when the answers were reset. */
    val protectedBlocks: MutableMap<Long, Boolean> = mutableMapOf()
    var protectionCheckedAt: Long = 0L

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
