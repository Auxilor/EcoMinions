package com.exanthiax.ecominions.minions.service

import com.willfp.eco.core.blocks.TestableBlock
import com.willfp.eco.core.blocks.impl.MaterialTestableBlock
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.TileState
import org.bukkit.block.data.Ageable

/**
 * Stocks a minion's work area with the blocks it mines, one block at a time.
 *
 * Only minions that opt in via `placement.self-seed` seed themselves; the rest
 * stay blocked until a player supplies blocks. Woodcutters are seeded by
 * [com.exanthiax.ecominions.minions.service.behaviour.WoodcutterBehaviour]
 * instead, which has a plant-wait-fell cycle of its own.
 */
class SeedingService(
    private val canWork: (Minion, Block) -> Boolean = { _, _ -> true }
) {
    /**
     * Seeds a single block, if there is anywhere left to seed.
     *
     * Runs before mining and takes the whole cycle, so the area is filled
     * before the first block is broken: pacing seeding on its own timer let the
     * minion mine whatever it had already placed whenever a seed was not yet
     * due, and the area never filled.
     *
     * @return true if a block was seeded, so the caller spends the cycle on
     * seeding rather than mining what was just placed.
     */
    fun tick(minion: Minion): Boolean {
        val placement = minion.state.definition.placement
        if (!placement.selfSeed) {
            return false
        }
        val seeds = placement.seedBlocks
        for (offset in seeds.indices) {
            val index = (minion.mining.nextSeedIndex + offset) % seeds.size
            val seed = seeds[index]
            val spot = findSpot(minion, seed, placement.surfaceBlock) ?: continue
            SeedSurface.prepare(SeedSurface.below(spot), placement.surfaceBlock)
            irrigate(minion, seed, spot)
            ripen(seed.place(spot.location))
            minion.mining.seeded += spot.location
            minion.mining.nextSeedIndex = index + 1
            return true
        }
        return false
    }

    /**
     * A seeded crop goes in fully grown, so it can be mined as soon as it is
     * placed rather than sitting unripe in a freshly stocked area.
     */
    private fun ripen(block: Block) {
        val data = block.blockData
        if (data is Ageable && data.age < data.maximumAge) {
            data.age = data.maximumAge
            block.blockData = data
        }
    }

    /**
     * @return true if there is somewhere left to seed, whether or not it is due.
     *
     * A minion with nothing to mine and a seed on the way is waiting, not
     * blocked, and should not be reported as if a player had to feed it.
     */
    fun isPending(minion: Minion): Boolean {
        val placement = minion.state.definition.placement
        return placement.selfSeed &&
            placement.seedBlocks.any { findSpot(minion, it, placement.surfaceBlock) != null }
    }

    /**
     * The nearest air block on ground [SeedSurface] accepts, never the minion's
     * own column. Ground that already has its surface wins over ground that
     * would have to be converted, so a prepared field is used before more of
     * the area is dug up.
     *
     * The search is [MinionWorkArea], the same box the minion mines in: seeding
     * above the box it can reach leaves ore stranded above the minion forever.
     */
    private fun findSpot(minion: Minion, seed: TestableBlock, expectedSurface: TestableBlock?): Block? {
        val radius = minion.stats.radius
        val center = minion.entity.location.block
        val world = minion.entity.world

        var best: Block? = null
        var bestDistance = Int.MAX_VALUE

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x == 0 && z == 0) {
                    continue
                }
                for (y in MinionWorkArea.verticalOffsets(minion)) {
                    val candidate = world.getBlockAt(center.x + x, center.y + y, center.z + z)
                    if (!isSeedable(minion, candidate, seed, expectedSurface)) {
                        continue
                    }
                    val distance = x * x + y * y + z * z +
                        if (SeedSurface.isPrepared(SeedSurface.below(candidate), expectedSurface)) 0 else UNPREPARED_PENALTY
                    if (distance < bestDistance) {
                        bestDistance = distance
                        best = candidate
                    }
                }
            }
        }
        return best
    }

    private fun isSeedable(minion: Minion, block: Block, seed: TestableBlock, expectedSurface: TestableBlock?): Boolean {
        if (block.type != Material.AIR) {
            return false
        }
        if (minion.mining.isAwaitingRegen(block)) {
            return false
        }
        if (minion.mining.isAwaitingRegen(SeedSurface.below(block))) {
            return false
        }
        val definition = minion.state.definition
        val placement = definition.placement
        val below = SeedSurface.below(block)
        return SeedSurface.accepts(below, expectedSurface, definition.targetBlocks) &&
            survives(minion, seed, block, below, expectedSurface) &&
            canWork(minion, block) &&
            (SeedSurface.isPrepared(below, expectedSurface) || canWork(minion, below)) &&
            PlantSpacing.isClear(minion, block, placement.spacing) {
                definition.targetBlocks.matches(it) || placement.seedBlocks.any { seed -> seed.matches(it) }
            }
    }

    /**
     * A vanilla seed only goes where it would stay, so a cactus is never set on
     * grass or a mushroom in full light to pop off as an item. Ground still to be
     * converted is judged once the surface is laid, so it is trusted here, as is
     * a water plant that can be given water of its own.
     */
    private fun survives(
        minion: Minion,
        seed: TestableBlock,
        block: Block,
        below: Block,
        expectedSurface: TestableBlock?
    ): Boolean {
        val material = (seed as? MaterialTestableBlock)?.material ?: return true
        if (!SeedSurface.isPrepared(below, expectedSurface)) {
            return true
        }
        if (material.createBlockData().isSupported(block)) {
            return true
        }
        return material in WATER_PLANTS && waterSpotFor(minion, below) != null
    }

    /** Lays a water source beside the ground of [spot] when [seed] needs one there. */
    private fun irrigate(minion: Minion, seed: TestableBlock, spot: Block) {
        val material = (seed as? MaterialTestableBlock)?.material ?: return
        if (material !in WATER_PLANTS || material.createBlockData().isSupported(spot)) {
            return
        }
        waterSpotFor(minion, SeedSurface.below(spot))?.let {
            it.type = Material.WATER
            minion.mining.seeded += it.location
        }
    }

    /**
     * Ground beside [ground] that can be turned into a water source without it
     * spreading: walled in on every side, open above, and never under the minion,
     * a plant, a tile entity or a block waiting on regen.
     */
    private fun waterSpotFor(minion: Minion, ground: Block): Block? {
        val center = minion.entity.location.block
        val targets = minion.state.definition.targetBlocks
        return HORIZONTAL.map { ground.getRelative(it) }.firstOrNull { candidate ->
            !(candidate.x == center.x && candidate.z == center.z) &&
                candidate.isSolid &&
                candidate.state !is TileState &&
                !targets.matches(candidate) &&
                !minion.mining.isAwaitingRegen(candidate) &&
                candidate.getRelative(BlockFace.UP).type == Material.AIR &&
                canWork(minion, candidate) &&
                HORIZONTAL.all { face ->
                    candidate.getRelative(face).let { it.isSolid || it.type == Material.WATER }
                }
        }
    }

    private companion object {
        /** Larger than any squared distance in range, so prepared ground always wins. */
        const val UNPREPARED_PENALTY = 1_000_000

        val WATER_PLANTS = setOf(Material.SUGAR_CANE)

        val HORIZONTAL = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    }
}
