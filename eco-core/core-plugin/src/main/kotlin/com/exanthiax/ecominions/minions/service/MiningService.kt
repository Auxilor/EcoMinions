package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.GameData
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionStats
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.BlockData
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
    private val gameData: GameData,
    private val treeFarming: TreeFarmingService,
    private val compactor: CompactorService,
    private val dropWarnings: DropWarningService,
    private val fuels: FuelService,
    private val pricing: SellPricingService
) {
    fun tick(minion: Minion, now: Long) {
        if (minion.entity.isDead || worlds.isDisabled(minion.entity.world)) {
            return
        }

        processRegen(minion, now)
        completePendingBreak(minion, now)

        if (now < minion.mining.nextActionAt) {
            return
        }

        if (minion.isStorageFull) {
            minion.animation = Minion.AnimationState.FULL
            minion.mining.nextActionAt = now + FULL_RETRY_TICKS
            return
        }

        val target = findTarget(minion)
        if (target == null) {
            handleNoTarget(minion, now)
            return
        }

        beginBreak(minion, target, now)
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

        if (target.type !in minion.state.type.targetBlocks) {
            return
        }
        breakBlock(minion, target, now)
    }

    private fun handleNoTarget(minion: Minion, now: Long) {
        val busy = minion.state.type.isTreeType && treeFarming.tick(minion, now)
        minion.animation = if (busy) Minion.AnimationState.IDLE else Minion.AnimationState.BLOCKED
        minion.mining.nextActionAt = now + effectiveSpeedTicks(minion)
    }

    private fun effectiveSpeedTicks(minion: Minion): Int =
        fuels.effectiveSpeedTicks(minion, minion.stats.speedTicks)

    private fun findTarget(minion: Minion): Block? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world
        val candidates = mutableListOf<Block>()

        for (x in -radius..radius) {
            for (y in -1..1) {
                for (z in -radius..radius) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (candidate.type in minion.state.type.targetBlocks && isMineableNow(candidate)) {
                        candidates += candidate
                    }
                }
            }
        }
        return candidates.randomOrNull()
    }

    /** Crops must be ripe, and stacked plants are only cut at their base. */
    private fun isMineableNow(block: Block): Boolean {
        val data = block.blockData
        if (data is Ageable && data.age < data.maximumAge) {
            return false
        }
        if (gameData.growthRules.isStackedColumn(block.type)) {
            val below = block.world.getBlockAt(block.x, block.y - 1, block.z)
            if (below.type == block.type) {
                return false
            }
        }
        return true
    }

    private fun breakBlock(minion: Minion, block: Block, now: Long) {
        val stats = minion.stats
        val original = block.type
        val column = if (gameData.growthRules.isStackedColumn(original)) collectColumn(block) else listOf(block)

        storeDrops(minion, column.flatMap { it.getDrops() })
        dropOverflow(minion, compactor.compact(minion.storage))

        for (segment in column.drop(1)) {
            segment.type = Material.AIR
        }

        if (minion.state.type.isTreeType) {
            block.type = Material.AIR
            return
        }

        queueRegen(minion, block, original, stats, now)
    }

    private fun storeDrops(minion: Minion, drops: List<ItemStack>) {
        for (drop in drops) {
            if (!pricing.isSellable(minion.state.type, drop.type)) {
                dropWarnings.warnUnsellable(minion, drop.type)
                continue
            }
            if (minion.storage.addItem(drop).isNotEmpty()) {
                break
            }
        }
    }

    private fun dropOverflow(minion: Minion, overflow: List<ItemStack>) {
        for (item in overflow) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }
    }

    private fun queueRegen(minion: Minion, block: Block, original: Material, stats: MinionStats, now: Long) {
        if (settings.value.showRegenMarker) {
            block.type = Material.BEDROCK
            minion.mining.pendingRegen += block.location
        } else {
            block.type = Material.AIR
        }

        minion.mining.regenQueue += MiningState.RegenEntry(
            location = block.location,
            regenData = fullyGrownData(original),
            surfaceBlock = minion.state.type.placement.surfaceBlock
                .takeIf { minion.state.type.placement.liftRegen },
            regenAt = now + stats.regenDelayTicks
        )
    }

    private fun processRegen(minion: Minion, now: Long) {
        val entries = minion.mining.regenQueue.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            if (now < entry.regenAt) {
                continue
            }
            entry.surfaceBlock?.let { surface ->
                val below = entry.location.clone().subtract(0.0, 1.0, 0.0).block
                if (below.type != surface) {
                    below.type = surface
                }
            }
            entry.location.block.blockData = entry.regenData
            minion.mining.pendingRegen.remove(entry.location)
            entries.remove()
        }
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

    private fun fullyGrownData(material: Material): BlockData {
        val data = material.createBlockData()
        if (data is Ageable) {
            data.age = data.maximumAge
        }
        return data
    }

    private companion object {
        const val FULL_RETRY_TICKS = 20L
    }
}
