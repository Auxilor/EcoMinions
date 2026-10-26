package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.testsupport.FakeIsland
import com.exanthiax.ecominions.testsupport.FakeIslandProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandMemberListenerTest {
    private val owner = UUID.randomUUID()
    private val member = UUID.randomUUID()
    private val island = FakeIsland("BentoBox", "island-one", owner, mutableSetOf(member))
    private val online = mutableSetOf(owner, member)
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
            activeWhileMemberOnline = true
        )
    )
    private val stats = MinionStats(MinionDefinitionFixture.build().baseStats, level = 1)
    private val minion = Minion(
        state = MinionState(definition = MinionDefinitionFixture.build(), owner = owner),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(stats.storageSize),
        stats = stats
    )
    private val minionService = mockk<MinionService>().also {
        every { it.getByUuid(minion.uuid) } returns minion
    }
    private val activity = mockk<MinionActivityService>(relaxed = true)
    private val registry = IslandRegistry { }.apply {
        register(FakeIslandProvider("BentoBox").also { it.claim(island) })
    }
    private val minionEntries = mutableListOf<String>()
    private val index = IslandMinionIndex(
        { minionEntries.toList() }, { minionEntries.clear(); minionEntries.addAll(it) },
        { emptyList() }, { }
    ).also { it.record(island, minion.uuid) }
    private val regions = mockk<Regions>().also {
        every { it.owns(any<Entity>()) } returns true
    }
    private val listener = IslandMemberListener(minionService, activity, registry, index, settings, regions)

    private fun player(uuid: UUID) = mockk<Player>().also { every { it.uniqueId } returns uuid }

    private fun join(uuid: UUID) = mockk<PlayerJoinEvent>().also { every { it.player } returns player(uuid) }

    private fun quit(uuid: UUID) = mockk<PlayerQuitEvent>().also { every { it.player } returns player(uuid) }

    @Test
    fun `a member joining resumes an idle island minion`() {
        every { activity.isActive(minion) } returns true

        listener.onJoin(join(member))

        verify(exactly = 1) { activity.resume(minion, any()) }
    }

    @Test
    fun `a member joining does not resume while the minion stays inactive`() {
        every { activity.isActive(minion) } returns false

        listener.onJoin(join(member))

        verify(exactly = 0) { activity.resume(any(), any()) }
    }

    @Test
    fun `toggle on idles when the last member leaves`() {
        every { activity.isActiveWithout(minion, member) } returns false

        listener.onQuit(quit(member))

        verify(exactly = 1) { activity.markIdle(minion, any()) }
    }

    @Test
    fun `a member leaving keeps the minion running while others are on`() {
        every { activity.isActiveWithout(minion, member) } returns true

        listener.onQuit(quit(member))

        verify(exactly = 0) { activity.markIdle(any(), any()) }
    }

    @Test
    fun `the owner is left to the owner listener`() {
        every { activity.isActive(minion) } returns true
        every { activity.isActiveWithout(minion, owner) } returns false

        listener.onJoin(join(owner))
        listener.onQuit(quit(owner))

        verify(exactly = 0) { activity.resume(any(), any()) }
        verify(exactly = 0) { activity.markIdle(any(), any()) }
    }

    @Test
    fun `nothing happens while the toggle is off`() {
        settings.update(settings.value.copy(activeWhileMemberOnline = false))
        every { activity.isActive(minion) } returns true
        every { activity.isActiveWithout(minion, member) } returns false

        listener.onJoin(join(member))
        listener.onQuit(quit(member))

        verify(exactly = 0) { activity.resume(any(), any()) }
        verify(exactly = 0) { activity.markIdle(any(), any()) }
    }
}
