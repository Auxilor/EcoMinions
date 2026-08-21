package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.libreforge.MinionDispatcher
import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.context.PlaceholderContext
import com.willfp.eco.util.NumberUtils
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConditionMinionHasUpgradeTest {

    private val type = MinionTypeFixture.build(allowedUpgradeIds = listOf("compactor"))

    @AfterEach
    fun tearDown() {
        unmockkStatic(NumberUtils::class)
    }

    private fun dispatcherFor(state: MinionState): MinionDispatcher {
        val minion = mockk<Minion> { every { this@mockk.state } returns state }
        return MinionDispatcher(minion)
    }

    private fun config(mode: String?, tier: Int? = null): Config {
        val config = mockk<Config>(relaxed = true)
        every { config.getString("upgrade") } returns "compactor"
        every { config.has("tier") } returns (tier != null)
        if (tier != null) {
            mockkStatic(NumberUtils::class)
            every { NumberUtils.evaluateExpression(any<String>(), any<PlaceholderContext>()) } returns tier.toDouble()
        }
        every { config.getStringOrNull("mode") } returns mode
        return config
    }

    private val holder = mockk<ProvidedHolder>(relaxed = true)

    @Test
    fun `default mode checks the owned tier`() {
        val state = MinionState(type = type)
            .withUpgrade("compactor", 2)
            .withActiveTier("compactor", 1)

        val met = ConditionMinionHasUpgrade.isMet(
            dispatcherFor(state), config(mode = null, tier = 2), holder, NoCompileData
        )

        assertTrue(met)
    }

    @Test
    fun `active mode checks the selected tier, not the owned tier`() {
        val state = MinionState(type = type)
            .withUpgrade("compactor", 2)
            .withActiveTier("compactor", 1)

        val met = ConditionMinionHasUpgrade.isMet(
            dispatcherFor(state), config(mode = "active", tier = 2), holder, NoCompileData
        )

        assertFalse(met)
    }

    @Test
    fun `active mode passes when the selected tier meets the threshold`() {
        val state = MinionState(type = type)
            .withUpgrade("compactor", 2)
            .withActiveTier("compactor", 2)

        val met = ConditionMinionHasUpgrade.isMet(
            dispatcherFor(state), config(mode = "active", tier = 2), holder, NoCompileData
        )

        assertTrue(met)
    }
}
