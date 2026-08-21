package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionUpgradeServiceTest {

    private val type = MinionTypeFixture.build(allowedUpgradeIds = listOf("compactor"))

    @AfterEach
    fun tearDown() {
        unmockkStatic(Bukkit::class)
    }

    private fun tier(level: Int) = UpgradeTier(level, value = 0.0, statDelta = "", costs = emptyList(), holder = null)

    private val compactor = Upgrade(
        id = "compactor", name = "Compactor", iconLookup = "furnace",
        tiers = mapOf(1 to tier(1), 2 to tier(2)), selectable = true
    )

    private val affordableCost = mockk<TierCost> {
        every { canAfford(any()) } returns true
        every { pay(any()) } just Runs
    }

    private fun minion(state: MinionState): Minion =
        mockk<Minion> { every { this@mockk.state } returns state }

    private fun service(minionService: MinionService = mockk(relaxed = true)) =
        MinionUpgradeService(minionService) to minionService

    @Test
    fun `buying a selectable upgrade's first tier also activates it`() {
        mockkStatic(Bukkit::class)
        every { Bukkit.getPluginManager() } returns mockk(relaxed = true)
        val (service, minionService) = service()
        val minion = minion(MinionState(type = type))
        val player = mockk<Player>(relaxed = true)

        service.purchase(player, minion, compactor, tier(1), affordableCost)

        verify {
            minionService.updateState(minion, withArg<MinionState> {
                assertEquals(1, it.upgradeLevel("compactor"))
                assertEquals(1, it.activeTierOf("compactor"))
            })
        }
    }

    @Test
    fun `activating an owned lower tier does not change the owned tier`() {
        val (service, minionService) = service()
        val state = MinionState(type = type).withUpgrade("compactor", 2).withActiveTier("compactor", 2)
        val minion = minion(state)

        val result = service.activate(minion, compactor, 1)

        assertEquals(MinionUpgradeService.ActivateResult.ACTIVATED, result)
        verify {
            minionService.updateState(minion, withArg<MinionState> {
                assertEquals(2, it.upgradeLevel("compactor"))
                assertEquals(1, it.activeTierOf("compactor"))
            })
        }
    }

    @Test
    fun `activating a tier refreshes the minion's holders`() {
        val (service, minionService) = service()
        val state = MinionState(type = type).withUpgrade("compactor", 2).withActiveTier("compactor", 2)
        val minion = minion(state)

        service.activate(minion, compactor, 1)

        verify { minionService.onLevelChanged(minion) }
    }

    @Test
    fun `activating a tier higher than owned is rejected`() {
        val (service, _) = service()
        val state = MinionState(type = type).withUpgrade("compactor", 1).withActiveTier("compactor", 1)
        val minion = minion(state)

        val result = service.activate(minion, compactor, 2)

        assertEquals(MinionUpgradeService.ActivateResult.TIER_NOT_OWNED, result)
    }

    @Test
    fun `activating a non-selectable upgrade is not allowed`() {
        val (service, _) = service()
        val speed = Upgrade(id = "speed", name = "Speed", iconLookup = "sugar", tiers = mapOf(1 to tier(1)))
        val state = MinionState(type = MinionTypeFixture.build(allowedUpgradeIds = listOf("speed")))
            .withUpgrade("speed", 1)
        val minion = minion(state)

        val result = service.activate(minion, speed, 1)

        assertEquals(MinionUpgradeService.ActivateResult.NOT_ALLOWED, result)
    }
}
