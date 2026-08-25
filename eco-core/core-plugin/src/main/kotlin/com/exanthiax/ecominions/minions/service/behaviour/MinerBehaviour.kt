package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Breaks a block and puts it back where it was.
 *
 * The safe default: regenerating never destroys what an owner built, so this is
 * what any block with no reason to behave otherwise gets.
 */
class MinerBehaviour(
    private val regen: RegenService,
    private val seeding: SeedingService
) : MinionBehaviour {

    override fun onBlockBroken(minion: Minion, block: Block, original: Material, now: Long) =
        regen.queue(minion, block, original, now)

    override fun onIdle(minion: Minion, now: Long): Boolean = seeding.tick(minion)
}
