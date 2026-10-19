package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.testsupport.FakeIsland
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandLimitsTest {
    private val ownerId = UUID.randomUUID()
    private val island = FakeIsland("fake", "island", ownerId)
    private var minionEntries = emptyList<String>()
    private var limitEntries = emptyList<String>()
    private val index = IslandMinionIndex(
        { minionEntries }, { minionEntries = it },
        { limitEntries }, { limitEntries = it }
    )
    private val enabledSettings = MinionSettings(
        hologramOffset = 1.6,
        hologramRefreshTicks = 20,
        invulnerable = true,
        showRegenMarker = true,
        regenMarkerFallback = Material.AIR,
        clearSeededOnPickup = false,
        hardSlotCap = 54,
        offlineEnabled = true,
        offlineEfficiency = 1.0,
        limitsEnabled = true,
        defaultLimit = 5,
        permissionOverrides = true,
        sellPricing = SellPricingSettings(autoFromEcoShop = false, multiplier = 1.0),
        islandLimitsEnabled = true,
        defaultIslandLimit = 10
    )
    private val settings = SettingsHolder(enabledSettings)
    private var owner: Player? = null
    private val limits = IslandLimits(settings, index) { owner }

    private fun ownerWith(vararg permissions: String): Player = mockk<Player> {
        every { effectivePermissions } returns permissions.map { granted ->
            mockk<PermissionAttachmentInfo> {
                every { permission } returns granted
                every { value } returns true
            }
        }.toSet()
    }

    @Test
    fun `island limits off means unlimited`() {
        settings.update(enabledSettings.copy(islandLimitsEnabled = false))

        assertEquals(Int.MAX_VALUE, limits.limitFor(island))
        assertFalse(limits.isFull(island))
    }

    @Test
    fun `the default applies with no overrides`() {
        owner = ownerWith()

        assertEquals(10, limits.limitFor(island))
    }

    @Test
    fun `the owner's highest island-limit permission wins`() {
        owner = ownerWith("ecominions.island-limit.15", "ecominions.island-limit.25")

        assertEquals(25, limits.limitFor(island))
    }

    @Test
    fun `permission overrides off keeps the default`() {
        settings.update(enabledSettings.copy(islandPermissionOverrides = false))
        owner = ownerWith("ecominions.island-limit.25")

        assertEquals(10, limits.limitFor(island))
    }

    @Test
    fun `an offline owner uses the cached limit`() {
        owner = ownerWith("ecominions.island-limit.25")
        limits.limitFor(island)
        owner = null

        assertEquals(25, limits.limitFor(island))
    }

    @Test
    fun `an offline owner with no cache uses the default`() {
        assertEquals(10, limits.limitFor(island))
    }

    @Test
    fun `an island is full once it holds its limit`() {
        owner = ownerWith()
        repeat(9) { index.record(island, UUID.randomUUID()) }
        assertFalse(limits.isFull(island))

        index.record(island, UUID.randomUUID())

        assertTrue(limits.isFull(island))
    }
}
