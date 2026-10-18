package com.exanthiax.ecominions.islands

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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandMinionIndexTest {
    private var minionEntries = emptyList<String>()
    private var limitEntries = emptyList<String>()
    private val index = IslandMinionIndex(
        { minionEntries }, { minionEntries = it },
        { limitEntries }, { limitEntries = it }
    )
    private val island = FakeIsland("BentoBox", "island-one", UUID.randomUUID())

    @Test
    fun `a minion is counted once on its island`() {
        val minion = UUID.randomUUID()

        index.record(island, minion)
        index.record(island, minion)

        assertEquals(1, index.count(island))
    }

    @Test
    fun `islands with the same id on different providers are separate`() {
        val other = FakeIsland("Lands", "island-one", UUID.randomUUID())

        index.record(island, UUID.randomUUID())
        index.record(island, UUID.randomUUID())
        index.record(other, UUID.randomUUID())

        assertEquals(2, index.count(island))
        assertEquals(1, index.count(other))
    }

    @Test
    fun `remove forgets the minion on every island`() {
        val minion = UUID.randomUUID()
        val other = FakeIsland("Lands", "island-two", UUID.randomUUID())
        index.record(island, minion)
        index.record(other, minion)
        index.record(island, UUID.randomUUID())

        index.remove(minion)

        assertEquals(1, index.count(island))
        assertEquals(0, index.count(other))
    }

    @Test
    fun `a cached limit is replaced, not duplicated`() {
        assertNull(index.cachedLimit(island))

        index.cacheLimit(island, 10)
        index.cacheLimit(island, 25)

        assertEquals(25, index.cachedLimit(island))
        assertEquals(1, limitEntries.size)
    }

    @Test
    fun `registering a minion records it on the island it stands on`() {
        val provider = FakeIslandProvider("BentoBox")
        val standLocation = Location(null, 4.0, 64.0, 9.0)
        provider.claim(island, standLocation)
        val registry = IslandRegistry { }.apply { register(provider) }
        val service = MinionService(
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true),
            onRegister = { minion -> registry.islandAt(minion.location)?.let { index.record(it, minion.uuid) } }
        )
        val definition = MinionDefinitionFixture.build()
        val stats = MinionStats(definition.baseStats, level = 1)
        val stand = mockk<ArmorStand>(relaxed = true)
        every { stand.location } returns standLocation
        val minion = Minion(
            state = MinionState(definition = definition),
            entity = stand,
            storage = MinionStorage(stats.storageSize) { a, b -> a.type == b.type },
            stats = stats
        )

        service.register(minion)

        assertEquals(1, index.count(island))
    }

    @Test
    fun `minionsOn lists the minions recorded on one island only`() {
        val other = FakeIsland("Lands", "island-one", UUID.randomUUID())
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()

        index.record(island, first)
        index.record(island, second)
        index.record(other, UUID.randomUUID())

        assertEquals(listOf(first, second), index.minionsOn(island))
    }
}
