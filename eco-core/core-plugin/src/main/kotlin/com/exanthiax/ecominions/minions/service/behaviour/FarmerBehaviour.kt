package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Harvests a ripe crop and puts a fresh one back.
 *
 * Identical to regenerating from the world's point of view - the crop is placed
 * again where it stood - but it is a separate type because a farmer only ever
 * takes ripe crops, and because what it replants is the block's `plant`, not
 * the block it broke.
 *
 * Deliberately not merged with [MinerBehaviour] despite the identical body:
 * the ripeness gate belongs here, and the two diverge as soon as it moves.
 */
class FarmerBehaviour(
    private val regen: RegenService,
    private val seeding: SeedingService
) : MinionBehaviour {

    override fun onBlockBroken(minion: Minion, block: Block, original: Material, now: Long) =
        regen.queue(minion, block, original, now)

    override fun onIdle(minion: Minion, now: Long): Boolean = seeding.tick(minion)
}
