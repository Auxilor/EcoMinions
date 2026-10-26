package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.testsupport.FakeIsland
import com.exanthiax.ecominions.testsupport.FakeIslandProvider
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandMinionTrackerTest {
    private var minionEntries = emptyList<String>()
    private val index = IslandMinionIndex({ minionEntries }, { minionEntries = it }, { emptyList() }, { })
    private val ownerId: UUID = UUID.randomUUID()
    private val island = FakeIsland("BentoBox", "island-one", ownerId)
    private val standLocation = Location(null, 4.0, 64.0, 9.0)
    private val registry = IslandRegistry { }
    private val members = IslandMemberCache()
    private val tracker = IslandMinionTracker(registry, index, members)
    private val minion = standMinion()

    private fun standMinion(): Minion {
        val definition = MinionDefinitionFixture.build()
        val stats = MinionStats(definition.baseStats, level = 1)
        val stand = mockk<ArmorStand>(relaxed = true)
        every { stand.location } returns standLocation
        return Minion(
            state = MinionState(definition = definition),
            entity = stand,
            storage = MinionStorage(stats.storageSize) { first, second -> first.type == second.type },
            stats = stats
        )
    }

    private fun claimed() = FakeIslandProvider("BentoBox").apply { claim(island, standLocation) }

    private fun failing() = mockk<IslandProvider> {
        every { id } returns "failing"
        every { islandAt(any()) } throws IllegalStateException("boom")
    }

    @Test
    fun `registering a minion records it on the island it stands on`() {
        registry.register(claimed())
        val service = MinionService(
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true),
            onRegister = tracker::register,
            onRetire = tracker::retire
        )

        service.register(minion)

        assertEquals(1, index.count(island))
        assertEquals(setOf(ownerId), members.membersOf(minion.uuid).toSet())

        service.retire(minion)

        assertEquals(0, index.count(island))
        assertTrue(members.membersOf(minion.uuid).isEmpty())
    }

    @Test
    fun `a minion no provider claims leaves the index`() {
        index.record(island, minion.uuid)
        registry.register(FakeIslandProvider("BentoBox"))

        tracker.register(minion)

        assertEquals(0, index.count(island))
    }

    @Test
    fun `a failing provider keeps the minion's entry`() {
        index.record(island, minion.uuid)
        registry.register(failing())

        tracker.register(minion)

        assertEquals(1, index.count(island))
        assertTrue(members.membersOf(minion.uuid).isEmpty())
    }

    @Test
    fun `no registered provider keeps the minion's entry`() {
        index.record(island, minion.uuid)

        tracker.register(minion)

        assertEquals(1, index.count(island))
    }
}
