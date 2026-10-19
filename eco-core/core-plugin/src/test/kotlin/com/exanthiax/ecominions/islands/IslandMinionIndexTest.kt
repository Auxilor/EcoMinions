package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.testsupport.FakeIsland
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
    fun `recording a minion on another island moves it there`() {
        val minion = UUID.randomUUID()
        val other = FakeIsland("BentoBox", "island-two", UUID.randomUUID())

        index.record(island, minion)
        index.record(other, minion)

        assertEquals(0, index.count(island))
        assertEquals(listOf(minion), index.minionsOn(other))
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
