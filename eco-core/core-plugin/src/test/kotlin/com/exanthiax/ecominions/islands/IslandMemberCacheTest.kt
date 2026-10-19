package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.MinionIsland
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandMemberCacheTest {
    private val ownerId: UUID = UUID.randomUUID()
    private val island = mockk<MinionIsland> { every { members() } returns listOf(ownerId) }
    private var now = 0L
    private val cache = IslandMemberCache { now }
    private val minion: UUID = UUID.randomUUID()

    @Test
    fun `members come from the island remembered for the minion`() {
        cache.remember(minion, island)

        assertEquals(listOf(ownerId), cache.membersOf(minion))
    }

    @Test
    fun `members are looked up once within the window`() {
        cache.remember(minion, island)
        cache.membersOf(minion)
        now = 5000L
        cache.membersOf(minion)

        verify(exactly = 1) { island.members() }
    }

    @Test
    fun `members refresh after the window`() {
        cache.remember(minion, island)
        cache.membersOf(minion)
        now = 5001L
        cache.membersOf(minion)

        verify(exactly = 2) { island.members() }
    }

    @Test
    fun `a minion with no island has no members`() {
        cache.remember(minion, null)

        assertTrue(cache.membersOf(minion).isEmpty())
        assertTrue(cache.membersOf(UUID.randomUUID()).isEmpty())
    }

    @Test
    fun `a forgotten minion has no members`() {
        cache.remember(minion, island)
        cache.membersOf(minion)
        cache.forget(minion)

        assertTrue(cache.membersOf(minion).isEmpty())
    }

    @Test
    fun `remembering a new island drops the cached members`() {
        val other: UUID = UUID.randomUUID()
        cache.remember(minion, island)
        cache.membersOf(minion)
        cache.remember(minion, mockk { every { members() } returns listOf(other) })

        assertEquals(listOf(other), cache.membersOf(minion))
    }
}
