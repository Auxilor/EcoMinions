package com.exanthiax.ecominions.testsupport

import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.entity.ArmorStand

/**
 * A block grid backed by a map, so block scans can be tested without a server.
 *
 * Coordinates are relative to the minion, which stands at the origin.
 */
class FakeWorld {
    private val blocks = HashMap<Triple<Int, Int, Int>, Material>()

    val bukkit: World = mockk<World>().also { world ->
        every { world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers {
            blockAt(firstArg(), secondArg(), thirdArg())
        }
    }

    fun set(x: Int, y: Int, z: Int, material: Material) {
        blocks[Triple(x, y, z)] = material
    }

    fun typeAt(x: Int, y: Int, z: Int): Material = blocks[Triple(x, y, z)] ?: Material.AIR

    fun countOf(material: Material): Int = blocks.values.count { it == material }

    /** Every position holding [material], as (x, y, z). */
    fun positionsOf(material: Material): List<Triple<Int, Int, Int>> =
        blocks.filterValues { it == material }.keys.toList()

    /** Fills y = -1 across a 5x5 area, leaving everything above it as air. */
    fun fillFloor(material: Material = Material.STONE) = fillLayer(-1, material)

    /** Fills one 5x5 layer, skipping the minion's own column at the origin. */
    fun fillLayer(y: Int, material: Material = Material.STONE) {
        for (x in -2..2) {
            for (z in -2..2) {
                if (y >= 0 && x == 0 && z == 0) {
                    continue
                }
                set(x, y, z, material)
            }
        }
    }

    fun blockAt(x: Int, y: Int, z: Int): Block = mockk<Block>().also { block ->
        every { block.x } returns x
        every { block.y } returns y
        every { block.z } returns z
        every { block.world } returns bukkit
        every { block.type } answers { typeAt(x, y, z) }
        every { block.isSolid } answers { typeAt(x, y, z) != Material.AIR }
        every { block.isPassable } answers { typeAt(x, y, z) == Material.AIR || typeAt(x, y, z) == Material.WATER }
        every { block.type = any() } answers { set(x, y, z, firstArg()) }
        // Real block data needs a running server; nothing under test is Ageable.
        every { block.blockData } returns mockk<BlockData>()
        // Drops need the item factory, which needs a server. Tests assert on
        // what the world looks like afterwards, not on the items.
        every { block.getDrops() } returns emptyList()
        // A plain state: nothing in a fake world is a tile entity.
        every { block.state } returns mockk<BlockState>()
        every { block.location } returns mockk {
            every { this@mockk.block } returns block
            every { this@mockk.world } returns bukkit
            every { this@mockk.blockX } returns x
            every { this@mockk.blockY } returns y
            every { this@mockk.blockZ } returns z
        }
    }

    /** An armour stand standing at the origin of this world. */
    fun stand(): ArmorStand = mockk<ArmorStand>().also { stand ->
        every { stand.world } returns bukkit
        every { stand.location } returns mockk<Location> {
            every { block } returns blockAt(0, 0, 0)
            every { blockX } returns 0
            every { blockY } returns 0
            every { blockZ } returns 0
        }
    }
}
