package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.testsupport.FakeIsland
import com.exanthiax.ecominions.testsupport.FakeIslandProvider
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionAccessTest {
    private val world = mockk<World> { every { name } returns "world" }
    private val location = Location(world, 1.0, 64.0, 2.0)
    private val outside = Location(world, 100.0, 64.0, 100.0)
    private val ownerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val island = FakeIsland("fake", "island", UUID.randomUUID(), mutableSetOf(memberId))
    private val registry = IslandRegistry { }.apply {
        register(FakeIslandProvider("fake").apply { claim(island, location) })
    }
    private val baseSettings = MinionSettings(
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
        sellPricing = SellPricingSettings(autoFromEcoShop = false, multiplier = 1.0)
    )
    private val settings = SettingsHolder(baseSettings)
    private val access = MinionAccess(registry, settings)

    private fun player(id: UUID, admin: Boolean = false) = mockk<Player> {
        every { uniqueId } returns id
        every { hasPermission("ecominions.admin") } returns admin
    }

    private fun minionAt(where: Location, owner: UUID? = ownerId) = mockk<Minion> {
        every { state } returns MinionState(definition = MinionDefinitionFixture.build(), owner = owner)
        every { this@mockk.location } returns where
    }

    @Test
    fun `the owner can always use and pick up their minion`() {
        val minion = minionAt(location)

        assertTrue(access.canUse(player(ownerId), minion))
        assertTrue(access.canPickUp(player(ownerId), minion))
        assertFalse(access.deniedByIsland(player(ownerId), minion, IslandPrivilege.USE))
    }

    @Test
    fun `a member with use can open but not pick up`() {
        island.allowed[memberId] = mutableSetOf(IslandPrivilege.USE)
        val minion = minionAt(location)

        assertTrue(access.canUse(player(memberId), minion))
        assertFalse(access.canPickUp(player(memberId), minion))
        assertTrue(access.deniedByIsland(player(memberId), minion, IslandPrivilege.PICKUP))
        assertFalse(access.deniedByIsland(player(memberId), minion, IslandPrivilege.USE))
    }

    @Test
    fun `a member with pickup can pick up`() {
        island.allowed[memberId] = mutableSetOf(IslandPrivilege.PICKUP)

        assertTrue(access.canPickUp(player(memberId), minionAt(location)))
    }

    @Test
    fun `outside an island only the owner can use`() {
        val minion = minionAt(outside)

        assertFalse(access.canUse(player(memberId), minion))
        assertFalse(access.deniedByIsland(player(memberId), minion, IslandPrivilege.USE))
        assertTrue(access.canUse(player(ownerId), minion))
    }

    @Test
    fun `privileges off keeps owner only access`() {
        island.allowed[memberId] = mutableSetOf(IslandPrivilege.USE, IslandPrivilege.PICKUP)
        settings.update(baseSettings.copy(islandPrivileges = false))
        val minion = minionAt(location)

        assertFalse(access.canUse(player(memberId), minion))
        assertFalse(access.deniedByIsland(player(memberId), minion, IslandPrivilege.USE))
        assertTrue(access.canUse(player(ownerId), minion))
    }

    @Test
    fun `placing needs the place privilege on an island`() {
        assertFalse(access.canPlace(player(memberId), location))

        island.allowed[memberId] = mutableSetOf(IslandPrivilege.PLACE)

        assertTrue(access.canPlace(player(memberId), location))
    }

    @Test
    fun `placing outside an island needs no privilege`() {
        assertTrue(access.canPlace(player(memberId), outside))
    }

    @Test
    fun `admins bypass island privileges`() {
        val admin = player(memberId, admin = true)

        assertTrue(access.canPlace(admin, location))
        assertTrue(access.canUse(admin, minionAt(location)))
        assertTrue(access.canPickUp(admin, minionAt(location)))
        assertFalse(access.deniedByIsland(admin, minionAt(location), IslandPrivilege.PICKUP))
    }

    @Test
    fun `an unowned minion is open to everyone`() {
        assertTrue(access.canUse(player(memberId), minionAt(location, owner = null)))
    }
}
