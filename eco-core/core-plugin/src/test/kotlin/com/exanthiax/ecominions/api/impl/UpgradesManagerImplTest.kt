package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.upgrade.PurchaseResult
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.exanthiax.ecominions.upgrades.service.UpgradeService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class UpgradesManagerImplTest {

    private val cheap = mockk<TierCost> { every { canAfford(any()) } returns false }
    private val dear = mockk<TierCost> { every { canAfford(any()) } returns true }

    private fun tier(level: Int, costs: List<TierCost>) =
        UpgradeTier(level = level, value = 1.0, statDelta = "", costs = costs, holder = null)

    private val speed = Upgrade(
        id = "speed",
        name = "Speed",
        iconLookup = "sugar",
        tiers = mapOf(1 to tier(1, listOf(cheap, dear)), 2 to tier(2, emptyList()))
    )

    private val uuid = UUID.randomUUID()
    private var state = MinionState(definition = MinionDefinitionFixture.build(allowedUpgradeIds = listOf("speed")))
    private val minion = mockk<Minion>(relaxed = true) {
        every { uuid } returns this@UpgradesManagerImplTest.uuid
        every { state } answers { this@UpgradesManagerImplTest.state }
        every { upgradeLevels } answers { this@UpgradesManagerImplTest.state.upgrades }
    }
    private val upgrades = UpgradeService(mockk(relaxed = true) { every { find("speed") } returns speed })
    private val purchases = mockk<MinionUpgradeService>(relaxed = true)
    private val minions = mockk<MinionService> { every { getByUuid(uuid) } returns minion }
    private val manager = UpgradesManagerImpl(upgrades, purchases, minions)
    private val player = mockk<Player>()

    @Test
    fun `only the next tier up can be bought`() {
        assertEquals(PurchaseResult.INVALID_TIER, manager.purchase(player, minion, speed, 2))
        verify(exactly = 0) { purchases.purchase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a tier with no cost cannot be bought`() {
        state = state.withUpgrade("speed", 1)

        assertEquals(PurchaseResult.NOT_ALLOWED, manager.purchase(player, minion, speed, 2))
    }

    @Test
    fun `the first affordable cost is charged`() {
        every { purchases.purchase(any(), any(), any(), any(), any()) } returns MinionUpgradeService.Result.PURCHASED

        assertEquals(PurchaseResult.PURCHASED, manager.purchase(player, minion, speed, 1))
        verify { purchases.purchase(player, minion, speed, speed.tier(1)!!, dear) }
    }

    @Test
    fun `every internal purchase result maps to a public one`() {
        val cases = mapOf(
            MinionUpgradeService.Result.PURCHASED to PurchaseResult.PURCHASED,
            MinionUpgradeService.Result.CANNOT_AFFORD to PurchaseResult.CANNOT_AFFORD,
            MinionUpgradeService.Result.NOT_ALLOWED to PurchaseResult.NOT_ALLOWED,
            MinionUpgradeService.Result.CONDITIONS_NOT_MET to PurchaseResult.CONDITIONS_NOT_MET,
            MinionUpgradeService.Result.CANCELLED to PurchaseResult.CANCELLED
        )

        for ((internal, expected) in cases) {
            every { purchases.purchase(any(), any(), any(), any(), any()) } returns internal
            assertEquals(expected, manager.purchase(player, minion, speed, 1))
        }
    }

    @Test
    fun `resetting sets every owned upgrade back to zero`() {
        state = state.withUpgrade("speed", 2)

        manager.resetUpgrades(minion)

        verify { purchases.setTier(minion, speed, 0) }
    }
}
