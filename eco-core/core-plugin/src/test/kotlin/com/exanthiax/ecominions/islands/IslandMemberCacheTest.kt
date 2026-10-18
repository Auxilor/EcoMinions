package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.testsupport.FakeIsland
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandMemberCacheTest {
    private val owner = UUID.randomUUID()
    private val island = FakeIsland("BentoBox", "island-one", owner)
    private val standLocation = Location(null, 4.0, 64.0, 9.0)
    private val provider = mockk<IslandProvider>().also {
        every { it.id } returns "BentoBox"
        every { it.islandAt(any()) } returns island
    }
    private val registry = IslandRegistry { }.apply { register(provider) }
    private var now = 0L
    private val cache = IslandMemberCache(registry) { now }
    private val minion = definitionMinion()

    private fun definitionMinion(): Minion {
        val definition = MinionDefinitionFixture.build()
        val stats = MinionStats(definition.baseStats, level = 1)
        val stand = mockk<ArmorStand>(relaxed = true)
        every { stand.location } returns standLocation
        return Minion(
            state = MinionState(definition = definition),
            entity = stand,
            storage = MinionStorage(stats.storageSize),
            stats = stats
        )
    }

    @Test
    fun `members are looked up once within the window`() {
        assertEquals(setOf(owner), cache.membersOf(minion).toSet())
        now = 5000L
        cache.membersOf(minion)

        verify(exactly = 1) { provider.islandAt(any()) }
    }

    @Test
    fun `members refresh after the window`() {
        cache.membersOf(minion)
        now = 5001L
        cache.membersOf(minion)

        verify(exactly = 2) { provider.islandAt(any()) }
    }

    @Test
    fun `a forgotten minion is looked up again`() {
        cache.membersOf(minion)
        cache.forget(minion.uuid)
        cache.membersOf(minion)

        verify(exactly = 2) { provider.islandAt(any()) }
    }
}
