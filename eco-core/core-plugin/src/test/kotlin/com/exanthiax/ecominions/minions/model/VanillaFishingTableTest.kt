package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class VanillaFishingTableTest {
    @Test
    fun `ten thousand rolls stay near vanilla weights`() {
        val random = Random(42)
        val rolls = (0 until 10_000).map { VanillaFishingTable.roll(random).type }

        val fish = rolls.count { it in VanillaFishingTable.fish } / 10_000.0
        val junk = rolls.count { it in VanillaFishingTable.junk } / 10_000.0
        val treasure = rolls.count { it in VanillaFishingTable.treasure } / 10_000.0

        assertTrue(fish in 0.83..0.87, "fish $fish")
        assertTrue(junk in 0.08..0.12, "junk $junk")
        assertTrue(treasure in 0.035..0.065, "treasure $treasure")
    }
}
