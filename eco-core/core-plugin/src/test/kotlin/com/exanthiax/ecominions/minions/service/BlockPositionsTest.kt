package com.exanthiax.ecominions.minions.service

import org.bukkit.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockPositionsTest {

    @Test
    fun `round trips positive and negative positions`() {
        for ((x, y, z) in listOf(Triple(0, 0, 0), Triple(700, -61, 700), Triple(-30000, 319, -12345), Triple(29999999, -64, -29999999))) {
            val unpacked = BlockPositions.unpack(null, BlockPositions.pack(Location(null, x.toDouble(), y.toDouble(), z.toDouble())))

            assertEquals(Triple(x, y, z), Triple(unpacked.blockX, unpacked.blockY, unpacked.blockZ))
        }
    }
}
