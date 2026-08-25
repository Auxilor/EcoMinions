package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.libreforge.conditions.ConditionList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionUpgradeServiceTest {

    private val type = MinionDefinitionFixture.build(allowedUpgradeIds = listOf("speed"))

    private fun tier(buyConditions: ConditionList = ConditionList(emptyList())) = UpgradeTier(
        level = 1,
        value = 5.0,
        statDelta = "-5 ticks",
        costs = emptyList(),
        holder = null,
        buyConditions = buyConditions
    )

    private val speed = Upgrade(id = "speed", name = "Speed", iconLookup = "sugar", tiers = mapOf(1 to tier()))

    private val affordableCost = mockk<TierCost> {
        every { canAfford(any()) } returns true
        every { pay(any()) } just Runs
    }

    private fun minion(state: MinionState): Minion =
        mockk<Minion>(relaxed = true) { every { this@mockk.state } returns state }

    private fun service(minionService: MinionService = mockk(relaxed = true)) =
        MinionUpgradeService(minionService) to minionService

    // No test for the PURCHASED result: a successful purchase calls
    // Bukkit.getPluginManager().callEvent(...), which needs a live server and
    // has never been unit-testable here (this class had no test file before
    // buy-conditions existed either). Verified manually instead.

    @Test
    fun `an upgrade not on the minion's allow-list is rejected`() {
        val (service, minionService) = service()
        val state = MinionState(type = MinionDefinitionFixture.build(allowedUpgradeIds = emptyList()))
        val minion = minion(state)
        val player = mockk<Player>(relaxed = true)

        val result = service.purchase(player, minion, speed, speed.tier(1)!!, affordableCost)

        assertEquals(MinionUpgradeService.Result.NOT_ALLOWED, result)
        verify(exactly = 0) { minionService.updateState(any(), any()) }
    }

    // No test for a non-empty, unmet buy-conditions list: evaluating that branch
    // constructs libreforge's `EmptyProvidedHolder`, which needs a live plugin
    // instance to initialize - same reason UpgradeLoader/CompactorService have
    // no unit tests either. Verified manually instead.

    @Test
    fun `an unaffordable tier is rejected once buy-conditions pass`() {
        val (service, _) = service()
        val minion = minion(MinionState(type = type))
        val player = mockk<Player>(relaxed = true)
        val unaffordable = mockk<TierCost> { every { canAfford(any()) } returns false }

        val result = service.purchase(player, minion, speed, speed.tier(1)!!, unaffordable)

        assertEquals(MinionUpgradeService.Result.CANNOT_AFFORD, result)
    }
}
