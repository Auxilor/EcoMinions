package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionUpgradeEvent
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionUpgradeServiceTest {

    private val type = MinionDefinitionFixture.build(allowedUpgradeIds = listOf("speed"))

    private fun tier(level: Int = 1, buyConditions: ConditionList = ConditionList(emptyList())) = UpgradeTier(
        level = level,
        value = 5.0,
        statDelta = "-5 ticks",
        costs = emptyList(),
        holder = null,
        buyConditions = buyConditions
    )

    private val speed = Upgrade(
        id = "speed",
        name = "Speed",
        iconLookup = "sugar",
        tiers = mapOf(1 to tier(1), 2 to tier(2), 3 to tier(3))
    )

    private val affordableCost = mockk<TierCost> {
        every { canAfford(any()) } returns true
        every { pay(any()) } just Runs
    }

    private val player = mockk<Player>(relaxed = true)

    private fun minion(state: MinionState): Minion =
        mockk<Minion>(relaxed = true) { every { this@mockk.state } returns state }

    private fun service(
        events: RecordingEventDispatcher = RecordingEventDispatcher(),
        minionService: MinionService = mockk(relaxed = true)
    ) = MinionUpgradeService(minionService, mockk<MinionTriggerDispatch>(relaxed = true), events) to minionService

    @Test
    fun `an upgrade not on the minion's allow-list is rejected`() {
        val (service, minionService) = service()
        val state = MinionState(definition = MinionDefinitionFixture.build(allowedUpgradeIds = emptyList()))

        val result = service.purchase(player, minion(state), speed, speed.tier(1)!!, affordableCost)

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
        val unaffordable = mockk<TierCost> { every { canAfford(any()) } returns false }

        val result = service.purchase(player, minion(MinionState(definition = type)), speed, speed.tier(1)!!, unaffordable)

        assertEquals(MinionUpgradeService.Result.CANNOT_AFFORD, result)
    }

    @Test
    fun `a purchase pays and applies the tier`() {
        val (service, minionService) = service()
        val minion = minion(MinionState(definition = type))

        val result = service.purchase(player, minion, speed, speed.tier(1)!!, affordableCost)

        assertEquals(MinionUpgradeService.Result.PURCHASED, result)
        verify { affordableCost.pay(player) }
        verify { minionService.updateState(minion, MinionState(definition = type).withUpgrade("speed", 1)) }
    }

    @Test
    fun `a cancelled purchase charges nothing and changes nothing`() {
        val (service, minionService) = service(RecordingEventDispatcher(allow = false))

        val result = service.purchase(player, minion(MinionState(definition = type)), speed, speed.tier(1)!!, affordableCost)

        assertEquals(MinionUpgradeService.Result.CANCELLED, result)
        verify(exactly = 0) { affordableCost.pay(any()) }
        verify(exactly = 0) { minionService.updateState(any(), any()) }
    }

    @Test
    fun `a listener's tier is the one applied`() {
        val events = RecordingEventDispatcher { (it as? MinionUpgradeEvent)?.tier = 3 }
        val (service, minionService) = service(events)
        val minion = minion(MinionState(definition = type))

        service.purchase(player, minion, speed, speed.tier(1)!!, affordableCost)

        verify { minionService.updateState(minion, MinionState(definition = type).withUpgrade("speed", 3)) }
    }

    @Test
    fun `setting a tier out of range fires nothing`() {
        val events = RecordingEventDispatcher()
        val (service, minionService) = service(events)

        val changed = service.setTier(minion(MinionState(definition = type)), speed, 4)

        assertFalse(changed)
        assertTrue(events.events.isEmpty())
        verify(exactly = 0) { minionService.updateState(any(), any()) }
    }

    @Test
    fun `setting a tier fires an event with no player`() {
        val events = RecordingEventDispatcher()
        val (service, _) = service(events)

        val changed = service.setTier(minion(MinionState(definition = type)), speed, 2)

        assertTrue(changed)
        assertEquals(null, events.last<MinionUpgradeEvent>().player)
        assertEquals(2, events.last<MinionUpgradeEvent>().tier)
    }
}
