package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.upgrades.model.TierCost
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.model.UpgradeTier
import com.willfp.libreforge.Holder
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionHoldersTest {

    private val tierOneHolder = mockk<Holder>()
    private val tierTwoHolder = mockk<Holder>()
    private val typeHolder = mockk<Holder>()

    private fun tier(level: Int, holder: Holder?) = UpgradeTier(
        level = level,
        value = level.toDouble(),
        statDelta = "",
        costs = emptyList<TierCost>(),
        holder = holder
    )

    private val speed = Upgrade(
        id = "speed",
        name = "Speed",
        iconLookup = "sugar",
        tiers = mapOf(1 to tier(1, tierOneHolder), 2 to tier(2, tierTwoHolder))
    )

    private fun minion(upgrades: Map<String, Int>, typeHolder: Holder?): Minion {
        val type = MinionDefinitionFixture.build(allowedUpgradeIds = listOf("speed"), holder = typeHolder)
        val state = MinionState(definition = type, upgrades = upgrades)
        return mockk<Minion> { every { this@mockk.state } returns state }
    }

    private val holders = MinionHolders { id -> if (id == "speed") speed else null }

    @Test
    fun `a minion with no upgrades and no type effects has no holders`() {
        assertTrue(holders.holdersFor(minion(emptyMap(), null)).isEmpty())
    }

    @Test
    fun `the type holder is always provided when the type declares one`() {
        val provided = holders.holdersFor(minion(emptyMap(), typeHolder))

        assertEquals(listOf(typeHolder), provided.map { it.holder })
    }

    @Test
    fun `only the owned tier's holder is provided`() {
        val provided = holders.holdersFor(minion(mapOf("speed" to 1), null))

        assertEquals(listOf(tierOneHolder), provided.map { it.holder })
    }

    @Test
    fun `purchasing a tier swaps which holder is provided`() {
        val provided = holders.holdersFor(minion(mapOf("speed" to 2), null))

        assertEquals(listOf(tierTwoHolder), provided.map { it.holder })
    }

    @Test
    fun `an upgrade the type does not allow is ignored`() {
        val type = MinionDefinitionFixture.build(allowedUpgradeIds = emptyList(), holder = null)
        val state = MinionState(definition = type, upgrades = mapOf("speed" to 1))
        val minion = mockk<Minion> { every { this@mockk.state } returns state }

        assertTrue(holders.holdersFor(minion).isEmpty())
    }

    @Test
    fun `an unknown upgrade id is ignored rather than throwing`() {
        assertTrue(holders.holdersFor(minion(mapOf("ghost" to 3), null)).isEmpty())
    }
}
