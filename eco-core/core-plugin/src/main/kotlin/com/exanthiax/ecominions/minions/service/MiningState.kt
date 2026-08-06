package com.exanthiax.ecominions.minions.service

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData

/** Where a single minion is up to in its mine/regen cycle. Never persisted. */
class MiningState {
    var nextActionAt: Long = 0L
    var nextAutoSellAt: Long = 0L

    /** The block being swung at, and the tick the swing lands on. */
    var pendingTarget: Block? = null
    var breakAt: Long = 0L

    /** Set while a tree minion is waiting for its sapling to mature. */
    var saplingPosition: Location? = null
    var saplingPlantedAt: Long = 0L

    val regenQueue: MutableList<RegenEntry> = mutableListOf()

    /** Blocks currently showing the regen marker, cleared if the minion is picked up. */
    val pendingRegen: MutableList<Location> = mutableListOf()

    class RegenEntry(
        val location: Location,
        val regenData: BlockData,
        val surfaceBlock: Material?,
        val regenAt: Long
    )
}
