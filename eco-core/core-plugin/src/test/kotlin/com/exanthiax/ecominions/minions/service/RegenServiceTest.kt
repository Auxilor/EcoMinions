package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegenServiceTest {

    /** Block data for the regenerated block needs a running server to build. */
    @BeforeEach
    fun stubBlockData() {
        mockkStatic(Bukkit::class)
        every { Bukkit.createBlockData(any<Material>()) } returns mockk<BlockData>()
    }

    /** The static mock is global, so it must not outlive this class. */
    @AfterEach
    fun releaseBlockData() = unmockkStatic(Bukkit::class)

    /** Bedrock read as a bug report; the marker is the stone the minion works in. */
    @Test
    fun `marks a broken block with the regen marker`() {
        val world = FakeWorld()
        val minion = minion(world)

        service(showMarker = true).queue(minion, world.blockAt(1, 0, 0), Material.COAL_ORE, now = 0L)

        assertEquals(RegenService.MARKER_BLOCK, world.typeAt(1, 0, 0))
    }

    @Test
    fun `leaves a hole when the marker is turned off`() {
        val world = FakeWorld()
        val minion = minion(world)

        service(showMarker = false).queue(minion, world.blockAt(1, 0, 0), Material.COAL_ORE, now = 0L)

        assertEquals(Material.AIR, world.typeAt(1, 0, 0))
    }

    private fun service(showMarker: Boolean) = RegenService(
        SettingsHolder(settings(showMarker)),
        mockk<MinionTriggerDispatch>(relaxed = true)
    )

    private fun minion(world: FakeWorld): Minion = mockk<Minion>().also { minion ->
        every { minion.state } returns mockk {
            every { this@mockk.definition } returns MinionDefinitionFixture.build()
        }
        every { minion.stats } returns mockk { every { this@mockk.regenDelayTicks } returns 60 }
        every { minion.entity } returns world.stand()
        every { minion.mining } returns MiningState()
    }

    private fun settings(showMarker: Boolean) = MinionSettings(
        hologramOffset = 1.6,
        hologramRefreshTicks = 20,
        invulnerable = true,
        showRegenMarker = showMarker,
        regenMarkerFallback = Material.AIR,
        clearSeededOnPickup = false,
        hardSlotCap = 54,
        offlineEnabled = true,
        offlineEfficiency = 1.0,
        limitsEnabled = true,
        defaultLimit = 5,
        permissionOverrides = true,
        sellPricing = SellPricingSettings(autoFromEcoShop = false, multiplier = 1.0)
    )
}
