package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.minions.service.activity.OfflineYield
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MinionDropListTest {
    private fun entry(chance: Double, amount: IntRange = 1..1, experience: IntRange? = null) =
        MinionDropList.Entry(ItemStack(Material.ROTTEN_FLESH), chance, amount, experience)

    @Test
    fun `a sure entry always drops and a zero entry never does`() {
        val list = MinionDropList(listOf(entry(1.0), entry(0.0)))

        repeat(50) { assertEquals(1, list.roll(Random(it)).drops.size) }
    }

    @Test
    fun `amounts stay inside the range`() {
        val list = MinionDropList(listOf(entry(1.0, amount = 2..4)))

        val amounts = (0 until 200).map { list.roll(Random(it)).drops.single().amount }

        assertTrue(amounts.all { it in 2..4 })
        assertEquals(setOf(2, 3, 4), amounts.toSet())
    }

    @Test
    fun `experience sums across hits`() {
        val list = MinionDropList(listOf(entry(1.0, experience = 2..2), entry(1.0, experience = 3..3)))

        assertEquals(5, list.roll(Random(1)).experience)
    }

    @Test
    fun `the empty list yields nothing`() {
        assertTrue(MinionDropList.EMPTY.isEmpty)
        assertEquals(OfflineYield(emptyList(), 0), MinionDropList.EMPTY.roll(Random(1)))
    }
}
