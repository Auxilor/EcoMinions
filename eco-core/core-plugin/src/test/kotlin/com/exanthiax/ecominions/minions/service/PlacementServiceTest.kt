package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.island.IslandPrivilege
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.islands.IslandLimits
import com.exanthiax.ecominions.islands.IslandMinionIndex
import com.exanthiax.ecominions.islands.IslandRegistry
import com.exanthiax.ecominions.islands.MinionAccess
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.OverLimitMode
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.testsupport.FakeIsland
import com.exanthiax.ecominions.testsupport.FakeIslandProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PlacementServiceTest {
    private val world = mockk<World>(relaxed = true) { every { name } returns "world" }
    private val location = Location(world, 1.0, 64.0, 2.0)
    private val ownerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val island = FakeIsland("fake", "island", ownerId, mutableSetOf(memberId))
    private val provider = spyk(FakeIslandProvider("fake").apply { claim(island, location) })
    private val registry = IslandRegistry { }.apply { register(provider) }
    private val settings = SettingsHolder(
        MinionSettings(
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
    )
    private var minionEntries = emptyList<String>()
    private var limitEntries = emptyList<String>()
    private val index = IslandMinionIndex(
        { minionEntries }, { minionEntries = it },
        { limitEntries }, { limitEntries = it }
    )
    private val minionService = mockk<MinionService>()
    private val events = mockk<EventDispatcher> { every { dispatch(any()) } returns false }
    private val placedData = mutableMapOf<UUID, List<String>>()
    private val placed = PlacedMinionRegistry({ placedData[it].orEmpty() }, { id, entries -> placedData[id] = entries })
    private val coal = MinionDefinitionFixture.build(id = "coal", limit = 2)
    private val definitions = listOf(MinionDefinitionFixture.build(), coal).associateBy { it.id }
    private val limits = MinionLimits(
        settings, mockk<PlacementLimits> { every { limitFor(any()) } returns 5 }, placed,
        { definitions[it] }, { definitions.values }, index, IslandLimits(settings, index) { null },
        { null }, { null }
    )
    private val placement = PlacementService(
        minionService, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
        limits, mockk(relaxed = true), settings, mockk(relaxed = true), events,
        MinionAccess(registry, settings), registry
    ) { _, _ -> true }
    private val state = MinionState(definition = MinionDefinitionFixture.build())

    private fun owns(owner: UUID, count: Int, typeId: String = "stone") =
        repeat(count) { placed.add(owner, UUID.randomUUID(), typeId) }

    private fun player(id: UUID, admin: Boolean = false): Player {
        val standing = Location(world, 0.0, 64.0, 0.0)
        return mockk<Player> {
            every { uniqueId } returns id
            every { this@mockk.location } returns standing
            every { hasPermission("ecominions.admin") } returns admin
        }
    }

    private fun fillIsland(count: Int) = repeat(count) { index.record(island, UUID.randomUUID()) }

    private fun placeAllowed(): Player {
        island.allowed[memberId] = mutableSetOf(IslandPrivilege.PLACE)
        return player(memberId)
    }

    @Test
    fun `placing on a full island is refused with the limit`() {
        val member = placeAllowed()
        owns(member.uniqueId, 0)
        fillIsland(10)

        assertEquals(PlacementService.PlaceResult.IslandLimitReached(10), placement.place(member, state, location))
    }

    @Test
    fun `admins bypass the island limit`() {
        val admin = player(memberId, admin = true)
        owns(admin.uniqueId, 0)
        fillIsland(10)

        assertEquals(PlacementService.PlaceResult.Cancelled, placement.place(admin, state, location))
    }

    @Test
    fun `per-player limit still applies on an island`() {
        val member = placeAllowed()
        owns(member.uniqueId, 5)
        fillIsland(1)

        assertEquals(PlacementService.PlaceResult.LimitReached(5), placement.place(member, state, location))
    }

    @Test
    fun `the island privilege is checked before the player limit`() {
        val member = player(memberId)
        owns(member.uniqueId, 5)

        assertEquals(PlacementService.PlaceResult.NoIslandPermission, placement.place(member, state, location))
    }

    @Test
    fun `placing looks the island up once`() {
        val member = placeAllowed()
        owns(member.uniqueId, 0)

        placement.place(member, state, location)

        verify(exactly = 1) { provider.islandAt(any()) }
    }

    @Test
    fun `placing with privileges and island limits off never looks the island up`() {
        settings.update(settings.value.copy(islandPrivileges = false, islandLimitsEnabled = false))
        val member = player(memberId)
        owns(member.uniqueId, 0)
        fillIsland(10)

        assertEquals(PlacementService.PlaceResult.Cancelled, placement.place(member, state, location))
        verify(exactly = 0) { provider.islandAt(any()) }
    }

    @Test
    fun `picking up without the pickup privilege reports no island permission`() {
        island.allowed[memberId] = mutableSetOf(IslandPrivilege.PLACE, IslandPrivilege.USE)
        val minionState = MinionState(definition = MinionDefinitionFixture.build(), owner = ownerId)
        val minionLocation = location
        val minion = mockk<Minion> {
            every { this@mockk.state } returns minionState
            every { this@mockk.location } returns minionLocation
        }

        assertEquals(PlacementService.PickUpResult.NoIslandPermission, placement.pickUp(player(memberId), minion))
    }

    @Test
    fun `placing past a minion limit is refused`() {
        val member = placeAllowed()
        owns(member.uniqueId, 2, "coal")

        assertEquals(
            PlacementService.PlaceResult.MinionLimitReached(coal, 2),
            placement.place(member, MinionState(definition = coal), location)
        )
    }

    @Test
    fun `placing past a type limit is refused`() {
        settings.update(settings.value.copy(typeLimitsEnabled = true, typeLimits = mapOf(MinionType.MINER to 2)))
        val member = placeAllowed()
        owns(member.uniqueId, 2)

        assertEquals(PlacementService.PlaceResult.TypeLimitReached(MinionType.MINER, 2), placement.place(member, state, location))
    }

    @Test
    fun `limits count the owner not the placer`() {
        val member = placeAllowed()
        owns(ownerId, 5)

        assertEquals(
            PlacementService.PlaceResult.LimitReached(5),
            placement.place(member, state.copy(owner = ownerId), location)
        )
    }

    @Test
    fun `inactive mode never refuses for limits`() {
        settings.update(settings.value.copy(overLimit = OverLimitMode.INACTIVE))
        val member = placeAllowed()
        owns(member.uniqueId, 5)
        fillIsland(10)

        assertEquals(PlacementService.PlaceResult.Cancelled, placement.place(member, state, location))
    }
}
