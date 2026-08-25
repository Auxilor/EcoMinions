package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.Material
import org.bukkit.block.Block

/**
 * What one [com.exanthiax.ecominions.minions.model.MinionType] actually does.
 *
 * Two hooks, because [com.exanthiax.ecominions.minions.service.MiningService]
 * only ever had two places where a tree minion differed from a mining one:
 * what happens to a block after it is broken, and what the minion does when
 * there is nothing in range to break.
 */
interface MinionBehaviour {
    /**
     * Called once the drops have been taken and the block is ready to be dealt
     * with. Implementations own what the block becomes: air, a regen entry, or
     * a replanted seedling.
     *
     * @param definition What [block] is, or null if nothing defines it.
     * @param original The material the block was before it was broken, needed
     * because [block] has already been emptied of its drops.
     */
    fun onBlockBroken(minion: Minion, block: Block, definition: MinionBlock?, original: Material, now: Long)

    /**
     * Called when the minion has nothing in range to break.
     *
     * @return true if the minion is busy (planting, waiting for growth), so it
     * idles rather than reporting that it is blocked.
     */
    fun onIdle(minion: Minion, now: Long): Boolean
}
