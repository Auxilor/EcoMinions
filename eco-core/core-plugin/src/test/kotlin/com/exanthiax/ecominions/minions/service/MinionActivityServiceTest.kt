package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.MinionTypeFixture
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionActivityServiceTest {

    private val owner = UUID.randomUUID()

    private val settings = SettingsHolder(
        MinionSettings(
            hologramOffset = 1.6,
            hologramRefreshTicks = 20,
            invulnerable = true,
            showRegenMarker = true,
            regenMarkerFallback = Material.AIR,
            hardSlotCap = 54,
            offlineEnabled = true,
            offlineEfficiency = 1.0,
            limitsEnabled = true,
            defaultLimit = 5,
            permissionOverrides = true,
            sellPricing = SellPricingSettings(autoFromEcoShop = false, multiplier = 1.0)
        )
    )

    private val stats = MinionStats(
        MinionType.BaseStats(
            speedTicks = 40,
            efficiencyTicks = 10,
            radius = 2,
            storageSlots = 1,
            maxStorageSlots = 54,
            regenDelayTicks = 60
        )
    )

    private fun minion(state: MinionState) = Minion(
        state = state,
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(stats.storageSize),
        stats = stats
    )

    private fun service(
        online: Set<UUID> = setOf(owner),
        repository: MinionStateRepository = mockk(relaxed = true),
        offlineProgress: OfflineProgressService = mockk(relaxed = true)
    ) = MinionActivityService(
        repository = repository,
        offlineProgress = offlineProgress,
        settings = settings,
        isOnline = { it in online }
    )

    @Test
    fun `a minion with an online owner is active`() {
        val subject = minion(MinionState(type = MinionTypeFixture.build(), owner = owner))

        assertTrue(service().isActive(subject))
    }

    @Test
    fun `a minion with an offline owner is inactive`() {
        val subject = minion(MinionState(type = MinionTypeFixture.build(), owner = owner))

        assertFalse(service(online = emptySet()).isActive(subject))
    }

    @Test
    fun `a minion with no owner is inactive`() {
        val subject = minion(MinionState(type = MinionTypeFixture.build(), owner = null))

        assertFalse(service().isActive(subject))
    }

    @Test
    fun `marking idle stamps the shutdown timestamp and freezes fuel`() {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val subject = minion(MinionState(type = MinionTypeFixture.build(), owner = owner))
        subject.burningFuelId = "coal"
        subject.burningFuelEndsAt = 5_000L

        service(repository = repository).markIdle(subject, now = 1_000L)

        assertEquals(1_000L, subject.state.shutdownTimestamp)
        assertEquals(4_000L, subject.burningFuelRemainingMillis)
        assertEquals(0L, subject.burningFuelEndsAt)
        verify(exactly = 1) { repository.markShutdown(subject, 1_000L) }
    }

    @Test
    fun `marking idle twice does not restamp`() {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val subject = minion(MinionState(type = MinionTypeFixture.build(), owner = owner))
        val service = service(repository = repository)

        service.markIdle(subject, now = 1_000L)
        service.markIdle(subject, now = 9_000L)

        assertEquals(1_000L, subject.state.shutdownTimestamp)
        verify(exactly = 1) { repository.markShutdown(subject, 1_000L) }
    }

    @Test
    fun `resuming credits the absence, clears the stamp and thaws fuel`() {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = minion(
            MinionState(type = MinionTypeFixture.build(), owner = owner, shutdownTimestamp = 1_000L)
        )
        subject.burningFuelId = "coal"
        subject.burningFuelRemainingMillis = 4_000L

        service(repository = repository, offlineProgress = offlineProgress)
            .resume(subject, now = 3_000L)

        verify(exactly = 1) { offlineProgress.apply(subject, 1_000L, 3_000L) }
        assertNull(subject.state.shutdownTimestamp)
        assertEquals(7_000L, subject.burningFuelEndsAt)
        verify(exactly = 1) { repository.clearShutdown(subject) }
    }

    @Test
    fun `resuming a minion that was never idle does nothing`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = minion(MinionState(type = MinionTypeFixture.build(), owner = owner))

        service(offlineProgress = offlineProgress).resume(subject, now = 3_000L)

        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
    }

    @Test
    fun `disabling offline calculation still clears the stamp`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        settings.update(settings.value.copy(offlineEnabled = false))
        val subject = minion(
            MinionState(type = MinionTypeFixture.build(), owner = owner, shutdownTimestamp = 1_000L)
        )

        service(offlineProgress = offlineProgress).resume(subject, now = 3_000L)

        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
        assertNull(subject.state.shutdownTimestamp)
        settings.update(settings.value.copy(offlineEnabled = true))
    }
}
