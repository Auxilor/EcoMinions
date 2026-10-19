package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.junit.jupiter.api.Test

class MinionServiceTest {
    private val activity = mockk<MinionActivityService>(relaxed = true)
    private val regions = mockk<Regions>(relaxed = true) {
        every { owns(any<Entity>()) } returns true
    }
    private val service = MinionService(
        mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
        activity, mockk(relaxed = true), mockk(relaxed = true), regions,
        mockk(relaxed = true)
    )

    private fun registeredMinion(): Minion {
        val definition = MinionDefinitionFixture.build()
        val stats = MinionStats(definition.baseStats, level = 1)
        val minion = Minion(
            state = MinionState(definition = definition),
            entity = mockk<ArmorStand>(relaxed = true),
            storage = MinionStorage(stats.storageSize) { a, b -> a.type == b.type },
            stats = stats
        )
        return service.register(minion)
    }

    @Test
    fun `refreshing activity resumes a minion that is now active`() {
        val minion = registeredMinion()
        every { activity.isActive(minion) } returns true

        service.refreshActivity(500L)

        verify { activity.resume(minion, 500L) }
        verify(exactly = 0) { activity.markIdle(any(), any()) }
    }

    @Test
    fun `refreshing activity idles a minion that is no longer active`() {
        val minion = registeredMinion()
        every { activity.isActive(minion) } returns false

        service.refreshActivity(500L)

        verify { activity.markIdle(minion, 500L) }
        verify(exactly = 0) { activity.resume(any(), any()) }
    }
}
