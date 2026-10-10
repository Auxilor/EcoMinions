package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.PlacementSettings
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionTest {

    private fun base(radius: String = "2", storageSlots: String = "3") = MinionDefinition.BaseStats(
        speedTicks = "40",
        efficiencyTicks = "10",
        radius = radius,
        storageSlots = storageSlots,
        maxStorageSlots = "54",
        hardSlotCap = 54,
        regenDelayTicks = "60"
    )

    private fun minion(definition: MinionDefinition): Minion {
        val stats = MinionStats(definition.baseStats, level = 1)
        return Minion(
            state = MinionState(definition = definition),
            entity = mockk<ArmorStand>(relaxed = true),
            // FakeItemStack does not fake isSimilar, so compare by material.
            storage = MinionStorage(stats.storageSize) { a, b -> a.type == b.type },
            stats = stats
        )
    }

    @Test
    fun `rebinding picks up a reloaded definition`() {
        val subject = minion(MinionDefinitionFixture.build(baseStats = base()))
        val reloaded = MinionDefinitionFixture.build(
            baseStats = base(radius = "5", storageSlots = "9"),
            placement = PlacementSettings(false, null, selfSeed = true, seedBlocks = listOf(mockk()))
        )

        val overflow = subject.rebind(reloaded)

        assertSame(reloaded, subject.state.definition)
        assertTrue(subject.state.definition.placement.selfSeed)
        assertEquals(5, subject.stats.radius)
        assertEquals(9, subject.storageSize)
        assertTrue(overflow.isEmpty())
    }

    @Test
    fun `rebinding to a smaller type hands back what no longer fits`() {
        val subject = minion(MinionDefinitionFixture.build(baseStats = base(storageSlots = "3")))
        subject.storage.add(ItemStack(Material.COBBLESTONE, 64))
        subject.storage.add(ItemStack(Material.DIRT, 64))
        subject.storage.add(ItemStack(Material.SAND, 64))

        val overflow = subject.rebind(MinionDefinitionFixture.build(baseStats = base(storageSlots = "1")))

        assertEquals(1, subject.storageSize)
        assertEquals(2, overflow.size)
    }
}
