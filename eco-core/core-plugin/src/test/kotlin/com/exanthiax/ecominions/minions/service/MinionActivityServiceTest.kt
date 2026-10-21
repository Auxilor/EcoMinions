package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.minion.LimitBreach
import com.exanthiax.ecominions.api.minion.LimitScope
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.MinionDefinition
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
            clearSeededOnPickup = false,
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

    private fun minion(state: MinionState) = Minion(
        state = state,
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(stats.storageSize),
        stats = stats
    )

    private fun service(
        online: Set<UUID> = setOf(owner),
        repository: MinionStateRepository = mockk(relaxed = true),
        offlineProgress: OfflineProgressService = mockk(relaxed = true),
        members: Collection<UUID> = emptyList(),
        limitBreach: (Minion) -> LimitBreach? = { null }
    ) = MinionActivityService(
        repository = repository,
        offlineProgress = offlineProgress,
        settings = settings,
        isOnline = { it in online },
        islandMembers = { members },
        limitBreach = limitBreach
    )

    private val breach = LimitBreach(LimitScope.MINION, 3, 2)

    private val member = UUID.randomUUID()

    private fun ownedMinion() = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = owner))

    private fun withToggle(enabled: Boolean, block: () -> Unit) {
        settings.update(settings.value.copy(activeWhileMemberOnline = enabled))
        try {
            block()
        } finally {
            settings.update(settings.value.copy(activeWhileMemberOnline = false))
        }
    }

    @Test
    fun `a minion with an online owner is active`() {
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = owner))

        assertTrue(service().isActive(subject))
    }

    @Test
    fun `a minion with an offline owner is inactive`() {
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = owner))

        assertFalse(service(online = emptySet()).isActive(subject))
    }

    @Test
    fun `a minion with no owner is inactive`() {
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = null))

        assertFalse(service().isActive(subject))
    }

    @Test
    fun `marking idle stamps the shutdown timestamp and freezes fuel`() {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = owner))
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
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = owner))
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
            MinionState(definition = MinionDefinitionFixture.build(), owner = owner, shutdownTimestamp = 1_000L)
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
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = owner))

        service(offlineProgress = offlineProgress).resume(subject, now = 3_000L)

        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
    }

    @Test
    fun `disabling offline calculation still clears the stamp`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        settings.update(settings.value.copy(offlineEnabled = false))
        val subject = minion(
            MinionState(definition = MinionDefinitionFixture.build(), owner = owner, shutdownTimestamp = 1_000L)
        )

        service(offlineProgress = offlineProgress).resume(subject, now = 3_000L)

        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
        assertNull(subject.state.shutdownTimestamp)
        settings.update(settings.value.copy(offlineEnabled = true))
    }

    @Test
    fun `toggle off keeps owner only activity`() = withToggle(false) {
        val subject = ownedMinion()

        assertFalse(service(online = setOf(member), members = listOf(owner, member)).isActive(subject))
    }

    @Test
    fun `toggle on runs while a member is online`() = withToggle(true) {
        val subject = ownedMinion()

        assertTrue(service(online = setOf(member), members = listOf(owner, member)).isActive(subject))
        assertFalse(service(online = emptySet(), members = listOf(owner, member)).isActive(subject))
    }

    @Test
    fun `toggle on never activates an unowned minion`() = withToggle(true) {
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = null))

        assertFalse(service(online = setOf(member), members = listOf(member)).isActive(subject))
    }

    @Test
    fun `leaving with the toggle off is inactive even for the owner`() = withToggle(false) {
        val subject = ownedMinion()

        assertFalse(service(online = setOf(owner, member), members = listOf(owner, member)).isActiveWithout(subject, owner))
    }

    @Test
    fun `the owner leaving keeps a minion running while members are on`() = withToggle(true) {
        val subject = ownedMinion()
        val subjectService = service(online = setOf(owner, member), members = listOf(owner, member))

        assertTrue(subjectService.isActiveWithout(subject, owner))
    }

    @Test
    fun `the last member leaving makes a minion inactive`() = withToggle(true) {
        val subject = ownedMinion()
        val subjectService = service(online = setOf(member), members = listOf(owner, member))

        assertFalse(subjectService.isActiveWithout(subject, member))
    }

    @Test
    fun `leaving does not count the leaver as online`() = withToggle(true) {
        val subject = ownedMinion()

        assertFalse(service(online = setOf(owner), members = listOf(owner)).isActiveWithout(subject, owner))
    }

    @Test
    fun `reconciling resumes a stamped minion that became active`() = withToggle(true) {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = minion(
            MinionState(definition = MinionDefinitionFixture.build(), owner = owner, shutdownTimestamp = 1_000L)
        )
        val subjectService = service(
            online = setOf(member), offlineProgress = offlineProgress, members = listOf(owner, member)
        )

        assertTrue(subjectService.reconcile(subject, now = 3_000L))

        verify(exactly = 1) { offlineProgress.apply(subject, 1_000L, 3_000L) }
        assertNull(subject.state.shutdownTimestamp)
    }

    @Test
    fun `reconciling stamps a running minion that became inactive`() = withToggle(true) {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val subject = ownedMinion()
        val subjectService = service(online = emptySet(), repository = repository, members = listOf(owner, member))

        assertFalse(subjectService.reconcile(subject, now = 2_000L))

        assertEquals(2_000L, subject.state.shutdownTimestamp)
        verify(exactly = 1) { repository.markShutdown(subject, 2_000L) }
    }

    @Test
    fun `reconciling a minion already in step changes nothing`() = withToggle(true) {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val idle = minion(
            MinionState(definition = MinionDefinitionFixture.build(), owner = owner, shutdownTimestamp = 1_000L)
        )
        val running = ownedMinion()

        service(online = emptySet(), repository = repository, offlineProgress = offlineProgress)
            .reconcile(idle, now = 2_000L)
        service(repository = repository, offlineProgress = offlineProgress).reconcile(running, now = 2_000L)

        assertEquals(1_000L, idle.state.shutdownTimestamp)
        assertNull(running.state.shutdownTimestamp)
        verify(exactly = 0) { repository.markShutdown(any(), any()) }
        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
    }

    @Test
    fun `an unowned minion is not stamped by the reconcile`() {
        val repository = mockk<MinionStateRepository>(relaxed = true)
        val subject = minion(MinionState(definition = MinionDefinitionFixture.build(), owner = null))

        assertFalse(service(repository = repository).reconcile(subject, now = 2_000L))

        assertNull(subject.state.shutdownTimestamp)
        verify(exactly = 0) { repository.markShutdown(any(), any()) }
    }

    @Test
    fun `a limit-idle minion is not active even with its owner online`() {
        assertFalse(service(limitBreach = { breach }).isActive(ownedMinion()))
    }

    @Test
    fun `owner joining does not resume or pay a limit-idle minion`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = ownedMinion()
        subject.state = subject.state.copy(shutdownTimestamp = 1_000L, limitedSince = 1_000L)

        service(offlineProgress = offlineProgress).resume(subject, now = 9_000L)

        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
        assertEquals(1_000L, subject.state.shutdownTimestamp)
    }

    @Test
    fun `limit-idle time is never paid`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = ownedMinion()
        var limited = true
        val service = service(offlineProgress = offlineProgress, limitBreach = { breach.takeIf { limited } })

        assertFalse(service.reconcile(subject, now = 1_000L))
        limited = false
        assertTrue(service.reconcile(subject, now = 5_000L))

        verify(exactly = 0) { offlineProgress.apply(any(), 1_000L, any()) }
        assertNull(subject.state.limitedSince)
        assertNull(subject.state.shutdownTimestamp)
    }

    @Test
    fun `offline window before becoming limited is paid when seen allowed`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = ownedMinion()
        var limited = false
        val service = service(online = emptySet(), offlineProgress = offlineProgress, limitBreach = { breach.takeIf { limited } })

        service.reconcile(subject, now = 1_000L)
        limited = true
        service.reconcile(subject, now = 3_000L)

        verify(exactly = 1) { offlineProgress.apply(subject, 1_000L, 3_000L) }
        assertEquals(3_000L, subject.state.limitedSince)
    }

    @Test
    fun `unloaded window is not paid when limited at wake`() {
        val offlineProgress = mockk<OfflineProgressService>(relaxed = true)
        val subject = ownedMinion()
        subject.state = subject.state.copy(shutdownTimestamp = 1_000L)

        service(offlineProgress = offlineProgress, limitBreach = { breach }).reconcile(subject, now = 3_000L)

        verify(exactly = 0) { offlineProgress.apply(any(), any(), any()) }
        assertEquals(3_000L, subject.state.limitedSince)
    }

    @Test
    fun `needsReconcile only when limit status changed`() {
        val subject = ownedMinion()
        var limited = false
        val service = service(limitBreach = { breach.takeIf { limited } })

        assertFalse(service.needsReconcile(subject))
        limited = true
        assertTrue(service.needsReconcile(subject))
        service.reconcile(subject, now = 1_000L)
        assertFalse(service.needsReconcile(subject))
    }
}
