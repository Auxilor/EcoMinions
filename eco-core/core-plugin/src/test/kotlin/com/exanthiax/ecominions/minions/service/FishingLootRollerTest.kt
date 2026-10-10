package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.integration.items.EcoItemsIntegrationService
import com.exanthiax.ecominions.minions.model.FishingSettings
import com.exanthiax.ecominions.minions.model.MinionDropList
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.activity.OfflineYield
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class FishingLootRollerTest {
    private val ecoItems = mockk<EcoItemsIntegrationService>()
    private val location = mockk<Location>(relaxed = true)
    private val owner = mockk<Player>(relaxed = true)

    private fun minion(drops: MinionDropList = MinionDropList.EMPTY) = mockk<Minion> {
        every { state } returns MinionState(
            definition = MinionDefinitionFixture.build(type = MinionType.FISHING, fishing = FishingSettings(1, drops))
        )
    }

    @Test
    fun `the drops list wins`() {
        val roller = FishingLootRoller(ecoItems, { _, _ -> error("vanilla not expected") }, Random(1))
        val drops = MinionDropList(listOf(MinionDropList.Entry(ItemStack(Material.DIAMOND), 1.0, 1..1, null)))

        assertEquals(Material.DIAMOND, roller.roll(minion(drops), location, owner).drops.single().type)
    }

    @Test
    fun `EcoItems is used for an online owner`() {
        every { ecoItems.rollFishing(location, owner) } returns OfflineYield(listOf(ItemStack(Material.NAUTILUS_SHELL)), 3)
        val roller = FishingLootRoller(ecoItems, { _, _ -> error("vanilla not expected") }, Random(1))

        val catch = roller.roll(minion(), location, owner)

        assertEquals(Material.NAUTILUS_SHELL, catch.drops.single().type)
        assertEquals(3, catch.experience)
    }

    @Test
    fun `vanilla is used offline, and the code table when vanilla gives nothing`() {
        val roller = FishingLootRoller(ecoItems, { _, _ -> null }, Random(1))

        val catch = roller.roll(minion(), location, null)

        assertEquals(1, catch.drops.size)
        assertTrue(catch.experience in 1..6)
    }
}
