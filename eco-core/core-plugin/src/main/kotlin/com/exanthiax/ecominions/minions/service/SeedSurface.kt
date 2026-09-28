package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.model.TargetSet
import com.willfp.eco.core.blocks.TestableBlock
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.block.data.type.Leaves

/**
 * The ground a self-seeder plants on.
 *
 * A minion with a surface block lays that surface itself rather than waiting
 * on a player to prepare the ground - a wheat minion tills its own farmland.
 * Only solid ground is converted, never air, so nothing is planted floating;
 * never a tile entity, so a chest in range is not paved over; and never one of
 * the minion's own targets, so a cactus is stacked on rather than turned to sand.
 * Leaves are left alone too, so a standing canopy is not grassed over.
 */
object SeedSurface {
    /** The ground under [block]. */
    fun below(block: Block): Block = block.world.getBlockAt(block.x, block.y - 1, block.z)

    /** The ground under [location]. */
    fun below(location: Location): Block =
        location.world!!.getBlockAt(location.blockX, location.blockY - 1, location.blockZ)

    /** @return true if a seed can go on top of [below], once [prepare]d. */
    fun accepts(below: Block, surface: TestableBlock?, targets: TargetSet): Boolean {
        if (surface == null) {
            return below.isSolid
        }
        if (surface.matches(below)) {
            return true
        }
        return below.isSolid &&
            below.blockData !is Leaves &&
            !targets.matches(below) &&
            below.state !is TileState
    }

    /** @return true if [below] needs nothing laid before a seed goes on it. */
    fun isPrepared(below: Block, surface: TestableBlock?): Boolean =
        surface == null || surface.matches(below)

    /** Lays [surface] at [below] if it is not already there. */
    fun prepare(below: Block, surface: TestableBlock?) {
        if (surface != null && !surface.matches(below)) {
            surface.place(below.location)
        }
    }
}
