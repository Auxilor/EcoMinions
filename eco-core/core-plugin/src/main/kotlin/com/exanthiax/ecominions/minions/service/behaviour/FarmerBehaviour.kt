package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Harvests a ripe crop and puts a young one back.
 *
 * The difference from [MinerBehaviour] is growth. Regenerating restores the
 * block exactly as it was - for a crop that means fully grown, the instant the
 * regen delay elapses, which makes the delay the only thing gating output.
 * Replanting places the crop at age zero and lets the world ripen it, so a
 * farmer is paced by the crop rather than by its own timer.
 *
 * A block that names no [MinionBlock.plant] has nothing to replant, so it falls
 * back to regenerating: the alternative is leaving a hole where the crop was.
 */
class FarmerBehaviour(
    private val regen: RegenService,
    private val seeding: SeedingService
) : MinionBehaviour {

    override fun onBlockBroken(
        minion: Minion,
        block: Block,
        definition: MinionBlock?,
        original: Material,
        now: Long
    ) {
        val plant = definition?.plant
        if (plant == null) {
            regen.queue(minion, block, original, now)
            return
        }
        plant.place(block.location)
    }

    override fun onIdle(minion: Minion, now: Long): Boolean = seeding.tick(minion)
}
