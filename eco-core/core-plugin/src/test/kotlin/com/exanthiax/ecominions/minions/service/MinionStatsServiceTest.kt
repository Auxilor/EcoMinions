package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.upgrade.UpgradesManager
import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionStatsServiceTest {

    private val speed = upgrade("speed", "speed", tierValues = mapOf(1 to 5.0, 2 to 15.0))
    private val capacity = upgrade("capacity", "capacity", tierValues = mapOf(1 to 2.0))
    private val forbidden = upgrade("fortune", "fortune", tierValues = mapOf(1 to 1.0))

    private val upgrades = mockk<UpgradesManager>()
    private val service = MinionStatsService(upgrades)

    private val type = MinionTypeFixture.build(allowedUpgradeIds = listOf("speed", "capacity"))

    init {
        every { upgrades.getById("speed") } returns speed
        every { upgrades.getById("capacity") } returns capacity
        every { upgrades.getById("fortune") } returns forbidden
        every { upgrades.getById(match { it !in setOf("speed", "capacity", "fortune") }) } returns null
        every { upgrades.effectValue(any(), any()) } answers {
            (firstArg<Upgrade>()).tier(secondArg())?.value
        }
    }

    @Test
    fun `purchased tiers are applied to base stats`() {
        val stats = service.statsFor(MinionState(type = type, upgrades = mapOf("speed" to 2)))

        assertEquals(25, stats.speedTicks)
    }

    @Test
    fun `upgrades the type does not allow are ignored`() {
        val stats = service.statsFor(MinionState(type = type, upgrades = mapOf("fortune" to 1)))

        assertEquals(1.0, stats.fortuneMultiplier)
    }

    @Test
    fun `upgrades that no longer exist in config are ignored`() {
        val stats = service.statsFor(MinionState(type = type, upgrades = mapOf("deleted" to 3)))

        assertEquals(40, stats.speedTicks)
    }

    @Test
    fun `a tier that is no longer configured is ignored`() {
        val stats = service.statsFor(MinionState(type = type, upgrades = mapOf("speed" to 9)))

        assertEquals(40, stats.speedTicks)
    }

    @Test
    fun `several upgrades apply together`() {
        val stats = service.statsFor(
            MinionState(type = type, upgrades = mapOf("speed" to 1, "capacity" to 1))
        )

        assertEquals(35, stats.speedTicks)
        assertEquals(3, stats.inventoryRows)
    }

    private fun upgrade(id: String, effect: String, tierValues: Map<Int, Double>) = Upgrade(
        id = id,
        name = id,
        effect = effect,
        iconLookup = "redstone",
        tiers = tierValues.mapValues { (level, value) ->
            UpgradeTier(level = level, value = value, statDelta = "", costs = emptyList())
        }
    )
}
