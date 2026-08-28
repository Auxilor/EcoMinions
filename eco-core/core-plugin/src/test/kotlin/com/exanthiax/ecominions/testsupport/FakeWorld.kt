package com.exanthiax.ecominions.testsupport

import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
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

    /** Fills y = -1 across a 5x5 area, leaving everything above it as air. */
    fun fillFloor(material: Material = Material.STONE) {
        for (x in -2..2) {
            for (z in -2..2) {
                set(x, -1, z, material)
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
        every { block.type = any() } answers { set(x, y, z, firstArg()) }
        // Real block data needs a running server; nothing under test is Ageable.
        every { block.blockData } returns mockk<BlockData>()
        every { block.location } returns mockk {
            every { this@mockk.block } returns block
        }
    }

    /** An armour stand standing at the origin of this world. */
    fun stand(): ArmorStand = mockk<ArmorStand>().also { stand ->
        every { stand.world } returns bukkit
        every { stand.location } returns mockk<Location> {
            every { block } returns blockAt(0, 0, 0)
        }
    }
}
