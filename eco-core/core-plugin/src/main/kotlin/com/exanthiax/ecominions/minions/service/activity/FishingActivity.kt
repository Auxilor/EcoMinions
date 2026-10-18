package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionCatchEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionCatchFish
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.FishingLootRoller
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionWorkArea
import com.willfp.libreforge.triggers.event.DropCause
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import kotlin.random.Random

/** Casts into water sources in its box. Self-seeding lays the pool one source at a time. */
class FishingActivity(
    private val roller: FishingLootRoller,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher,
    private val experience: ExperienceSink,
    private val ownerOf: (Minion) -> Player? = { it.ownerPlayer() },
    private val listening: (HandlerList) -> Boolean = { it.registeredListeners.isNotEmpty() },
    private val isWaterSource: (Block) -> Boolean = ::isStillWater,
    private val splash: (Block) -> Unit = { it.world.spawnParticle(Particle.SPLASH, it.location.add(0.5, 1.0, 0.5), SPLASH_COUNT) },
    private val random: Random = Random.Default
) : MinionActivity {

    override fun onTick(minion: Minion, now: Long) {
        val water = minion.mining.pendingCatch ?: return
        if (now < minion.mining.breakAt) {
            return
        }
        minion.mining.pendingCatch = null
        if (isWaterSource(water)) {
            catch(minion, water)
        }
    }

    override fun act(minion: Minion, now: Long): ActivityResult {
        val settings = minion.state.definition.fishing ?: return ActivityResult.BLOCKED
        if (minion.state.definition.placement.selfSeed && !minion.mining.seededLastCycle && seed(minion)) {
            return ActivityResult.SEEDED
        }

        val water = waterOf(minion, settings.minWater).takeIf { it.size >= settings.minWater }?.random(random)
        if (water == null) {
            minion.blockedReason = NO_WATER
            return ActivityResult.BLOCKED
        }

        minion.mining.pendingCatch = water
        minion.mining.breakAt = now + minion.stats.efficiencyTicks
        splash(water)
        return ActivityResult.WORKED
    }

    override fun hasWork(minion: Minion): Boolean {
        val settings = minion.state.definition.fishing ?: return false
        return minion.state.definition.placement.selfSeed || scan(minion).size >= settings.minWater
    }

    override fun offlineCycle(minion: Minion): OfflineYield? {
        val settings = minion.state.definition.fishing ?: return null
        if (minion.mining.waterBlocks.isEmpty()) {
            minion.mining.waterBlocks = scan(minion)
        }
        val water = minion.mining.waterBlocks.takeIf { it.size >= settings.minWater }?.random(random) ?: return null
        return roller.roll(minion, water.location, null)
    }

    private fun catch(minion: Minion, water: Block) {
        val roll = roller.roll(minion, water.location, ownerOf(minion))
        val event = MinionCatchEvent(minion, water, roll.drops.toMutableList(), roll.experience)
        if (listening(MinionCatchEvent.getHandlerList()) && !events.dispatch(event)) {
            return
        }
        val result = dispatch.drops(
            minion = minion,
            trigger = TriggerMinionCatchFish,
            drops = event.drops,
            block = water,
            location = water.location,
            cause = DropCause.CUSTOM
        )
        dispatch.storeAndDispatchCollect(minion, result.results.map { (item, _) -> item })
        experience.add(minion, event.experience + result.results.sumOf { it.xp } + result.contributedExperience)
    }

    /** Cached between rescans, except while short of water, so a pool built by hand is found on the next cycle. */
    private fun waterOf(minion: Minion, minWater: Int): List<Block> {
        val mining = minion.mining
        if (mining.waterScanDue <= 0 || mining.waterBlocks.size < minWater) {
            mining.waterBlocks = scan(minion)
            mining.waterScanDue = RESCAN_CYCLES
        }
        mining.waterScanDue--
        return mining.waterBlocks
    }

    private fun scan(minion: Minion): List<Block> = MinionWorkArea.blocks(minion).filter(isWaterSource)

    private fun seed(minion: Minion): Boolean {
        val spot = MinionWorkArea.blocks(minion)
            .filter { (it.type == Material.AIR || isFlowingWater(it)) && it.world.getBlockAt(it.x, it.y - 1, it.z).isSolid }
            .randomOrNull(random)
            ?: return false
        spot.type = Material.WATER
        minion.mining.seeded += spot.location
        minion.mining.waterScanDue = 0
        return true
    }

    private companion object {
        const val NO_WATER = "no-water"
        const val RESCAN_CYCLES = 20
        const val SPLASH_COUNT = 8
    }
}

private fun isStillWater(block: Block): Boolean =
    block.type == Material.WATER && (block.blockData as? Levelled)?.level == 0

private fun isFlowingWater(block: Block): Boolean =
    block.type == Material.WATER && !isStillWater(block)
