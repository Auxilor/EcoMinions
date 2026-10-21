package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.minion.LimitBreach
import com.exanthiax.ecominions.api.minion.LimitScope
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.islands.IslandLimits
import com.exanthiax.ecominions.islands.IslandMinionIndex
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.OverLimitMode
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.testsupport.FakeIsland
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionLimitsTest {
    private val owner = UUID.randomUUID()
    private val placedData = mutableMapOf<UUID, List<String>>()
    private val registry = PlacedMinionRegistry({ placedData[it].orEmpty() }, { id, entries -> placedData[id] = entries })
    private var islandEntries = emptyList<String>()
    private var islandLimitEntries = emptyList<String>()
    private val index = IslandMinionIndex(
        { islandEntries }, { islandEntries = it },
        { islandLimitEntries }, { islandLimitEntries = it }
    )
    private val online = mutableMapOf<UUID, Player>()
    private var islandHere: MinionIsland? = null

    private val coal = MinionDefinitionFixture.build(id = "coal", limit = 2, name = "Coal Minion")
    private val iron = MinionDefinitionFixture.build(id = "iron")
    private val stone = MinionDefinitionFixture.build(id = "stone")
    private val wheat = MinionDefinitionFixture.build(id = "wheat", type = MinionType.FARMER)
    private val definitions = listOf(coal, iron, stone, wheat).associateBy { it.id }

    private fun settings(
        overLimit: OverLimitMode = OverLimitMode.INACTIVE,
        defaultLimit: Int = 5,
        typeLimits: Map<MinionType, Int> = emptyMap(),
        islandLimitsEnabled: Boolean = false,
        defaultIslandLimit: Int = 10
    ) = MinionSettings(
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
        defaultLimit = defaultLimit,
        permissionOverrides = true,
        sellPricing = SellPricingSettings(autoFromEcoShop = false, multiplier = 1.0),
        islandLimitsEnabled = islandLimitsEnabled,
        defaultIslandLimit = defaultIslandLimit,
        overLimit = overLimit,
        typeLimitsEnabled = typeLimits.isNotEmpty(),
        typeLimits = typeLimits
    )

    private fun limits(settings: MinionSettings = settings()): MinionLimits {
        val holder = SettingsHolder(settings)
        return MinionLimits(
            holder, PlacementLimits(holder), registry, { definitions[it] }, { definitions.values },
            index, IslandLimits(holder, index) { online[it] }, { islandHere }, { online[it] }
        )
    }

    private val stats = MinionStats(
        MinionDefinition.BaseStats(
            speedTicks = "40",
            efficiencyTicks = "10",
            radius = "2",
            storageSlots = "1",
            maxStorageSlots = "54",
            hardSlotCap = 54,
            regenDelayTicks = "60"
        ),
        level = 1
    )

    private fun place(definition: MinionDefinition, by: UUID = owner): Minion {
        val uuid = UUID.randomUUID()
        registry.add(by, uuid, definition.id)
        islandHere?.let { index.record(it, uuid) }
        return Minion(
            state = MinionState(definition = definition, owner = by, uuid = uuid),
            entity = mockk<ArmorStand>(relaxed = true),
            storage = MinionStorage(stats.storageSize),
            stats = stats
        )
    }

    private fun player(id: UUID = owner, vararg permissions: String, admin: Boolean = false): Player = mockk<Player> {
        every { uniqueId } returns id
        every { hasPermission("ecominions.admin") } returns admin
        every { effectivePermissions } returns permissions.map { permission ->
            mockk<PermissionAttachmentInfo> {
                every { this@mockk.permission } returns permission
                every { value } returns true
            }
        }.toSet()
    }.also { online[id] = it }

    @Test
    fun `newest placed is the one over the minion limit`() {
        val limits = limits()
        val first = place(coal)
        val second = place(coal)
        val third = place(coal)

        assertNull(limits.breachOf(first))
        assertNull(limits.breachOf(second))
        assertEquals(LimitBreach(LimitScope.MINION, 3, 2), limits.breachOf(third))
    }

    @Test
    fun `picking one up lets the next one in`() {
        val limits = limits()
        val first = place(coal)
        place(coal)
        val third = place(coal)
        limits.breachOf(third)

        registry.remove(owner, first.uuid)
        limits.invalidate(owner)

        assertNull(limits.breachOf(third))
    }

    @Test
    fun `a minion idle under one limit does not count toward others`() {
        val limits = limits(settings(defaultLimit = 5))
        place(coal)
        place(coal)
        val extraCoal = place(coal)
        val irons = List(3) { place(iron) }

        assertEquals(LimitScope.MINION, limits.breachOf(extraCoal)?.scope)
        irons.forEach { assertNull(limits.breachOf(it)) }
    }

    @Test
    fun `the total limit idles the newest minion`() {
        val limits = limits(settings(defaultLimit = 2))
        place(iron)
        place(stone)
        val third = place(iron)

        assertEquals(LimitBreach(LimitScope.PLAYER, 3, 2), limits.breachOf(third))
    }

    @Test
    fun `type limit counts every minion of that type`() {
        val limits = limits(settings(typeLimits = mapOf(MinionType.MINER to 2)))
        place(coal)
        place(iron)
        val farmer = place(wheat)
        val third = place(stone)

        assertNull(limits.breachOf(farmer))
        assertEquals(LimitBreach(LimitScope.TYPE, 3, 2), limits.breachOf(third))
    }

    @Test
    fun `permission raises a minion cap and never lowers it`() {
        player(owner, "ecominions.minion-limit.coal.4")
        assertEquals(4, limits().capsFor(owner).minionCap("coal"))

        player(owner, "ecominions.minion-limit.coal.1")
        assertEquals(2, limits().capsFor(owner).minionCap("coal"))
    }

    @Test
    fun `permission raises a type cap`() {
        player(owner, "ecominions.type-limit.miner.7")

        assertEquals(7, limits(settings(typeLimits = mapOf(MinionType.MINER to 2))).capsFor(owner).typeCap(MinionType.MINER))
    }

    @Test
    fun `admin owners have no caps`() {
        player(owner, admin = true)
        val caps = limits().capsFor(owner)

        assertEquals(PlacementLimits.UNLIMITED, caps.total)
        assertEquals(PlacementLimits.UNLIMITED, caps.minionCap("coal"))
    }

    @Test
    fun `offline owner uses last seen caps then defaults`() {
        val limits = limits()
        player(owner, "ecominions.minion-limit.coal.4")
        limits.refreshCaps(online.getValue(owner))
        online.remove(owner)

        assertEquals(4, limits.capsFor(owner).minionCap("coal"))
        assertEquals(2, limits().capsFor(owner).minionCap("coal"))
    }

    @Test
    fun `unknown minion id counts toward the total only`() {
        val limits = limits(settings(defaultLimit = 2))
        registry.add(owner, UUID.randomUUID(), "deleted")
        place(coal)
        val third = place(coal)

        assertEquals(LimitBreach(LimitScope.PLAYER, 3, 2), limits.breachOf(third))
    }

    @Test
    fun `breachOf is null in prevent mode`() {
        val limits = limits(settings(overLimit = OverLimitMode.PREVENT))
        place(coal)
        place(coal)

        assertNull(limits.breachOf(place(coal)))
    }

    @Test
    fun `checkPlacement reports the first failing limit in order`() {
        val placer = player()
        val limits = limits(settings(overLimit = OverLimitMode.PREVENT, defaultLimit = 2, typeLimits = mapOf(MinionType.MINER to 2)))
        place(coal)
        place(coal)

        assertEquals(LimitBreach(LimitScope.PLAYER, 3, 2), limits.checkPlacement(placer, owner, coal) { null })
    }

    @Test
    fun `checkPlacement reports type before minion`() {
        val placer = player()
        val limits = limits(settings(overLimit = OverLimitMode.PREVENT, typeLimits = mapOf(MinionType.MINER to 2)))
        place(coal)
        place(coal)

        assertEquals(LimitBreach(LimitScope.TYPE, 3, 2), limits.checkPlacement(placer, owner, coal) { null })
        assertNull(limits.checkPlacement(placer, owner, wheat) { null })
    }

    @Test
    fun `checkPlacement counts the owner not the placer`() {
        val placer = player(UUID.randomUUID())
        val limits = limits(settings(overLimit = OverLimitMode.PREVENT))
        place(coal)
        place(coal)

        assertEquals(LimitBreach(LimitScope.MINION, 3, 2), limits.checkPlacement(placer, owner, coal) { null })
        assertNull(limits.checkPlacement(placer, placer.uniqueId, coal) { null })
    }

    @Test
    fun `island pass ranks by island order`() {
        val island = FakeIsland("fake", "island", UUID.randomUUID())
        islandHere = island
        val limits = limits(settings(islandLimitsEnabled = true, defaultIslandLimit = 2))
        place(iron, UUID.randomUUID())
        place(iron, UUID.randomUUID())
        val third = place(iron)

        assertEquals(LimitBreach(LimitScope.ISLAND, 3, 2), limits.breachOf(third))
    }

    @Test
    fun `inactive counts the owner's limit-idle minions`() {
        val limits = limits()
        repeat(4) { place(coal) }

        assertEquals(2, limits.inactiveCount(owner))
    }

    @Test
    fun `in use only when a new limit or inactive mode is set`() {
        assertEquals(true, limits().inUse())
    }
}
