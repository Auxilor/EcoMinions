package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionRegenEvent
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.BukkitEventDispatcher
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionRegenBlock
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.activity.BlockActivity
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.BlockData

/**
 * The break-then-put-it-back half of mining, owned by the types that regenerate.
 *
 * Split out of [BlockActivity] so a behaviour can queue a regen without the
 * whole mining cycle, and so a minion that never regenerates never reaches
 * this code.
 */
class RegenService(
    private val settings: SettingsHolder<MinionSettings>,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher = BukkitEventDispatcher
) {
    fun queue(minion: Minion, block: Block, original: Material, now: Long) {
        if (settings.value.showRegenMarker) {
            block.type = MARKER_BLOCK
            minion.mining.pendingRegen += block.location
        } else {
            block.type = Material.AIR
        }

        minion.mining.regenQueue += MiningState.RegenEntry(
            location = block.location,
            regenData = fullyGrownData(original),
            surfaceBlock = minion.state.definition.placement.surfaceBlock
                .takeIf { minion.state.definition.placement.liftRegen },
            regenAt = now + minion.stats.regenDelayTicks
        )
    }

    fun process(minion: Minion, now: Long) {
        val entries = minion.mining.regenQueue.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            if (now < entry.regenAt) {
                continue
            }
            entry.surfaceBlock?.let { surface ->
                val below = entry.location.clone().subtract(0.0, 1.0, 0.0).block
                if (!surface.matches(below)) {
                    surface.place(below.location)
                }
            }
            entry.location.block.blockData = entry.regenData
            dispatch.trigger(minion, TriggerMinionRegenBlock, block = entry.location.block)
            if (MinionRegenEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                events.dispatch(MinionRegenEvent(minion, entry.location.block))
            }
            minion.mining.pendingRegen.remove(entry.location)
            entries.remove()
        }
    }

    companion object {
        /**
         * The block standing in for one being regenerated.
         *
         * Cobblestone rather than bedrock: bedrock in an ordinary build reads as
         * a broken world. Nothing mines it while it stands, because a block on
         * the regen queue is skipped by both the target finder and the seeder.
         */
        val MARKER_BLOCK: Material = Material.COBBLESTONE
    }

    private fun fullyGrownData(material: Material): BlockData {
        val data = material.createBlockData()
        if (data is Ageable) {
            data.age = data.maximumAge
        }
        return data
    }
}
