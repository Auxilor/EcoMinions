package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionProtectionTest {

    private val world = FakeWorld()
    private val owner = mockk<Player> { every { uniqueId } returns UUID.randomUUID() }
    private val member = mockk<Player> { every { uniqueId } returns UUID.randomUUID() }
    private var builder = owner
    private var askedFor = emptyList<Player>()
    private val minion = minionOf(bypassAntigrief = false)
    private var now = 0L
    private var asked = 0

    private fun minionOf(bypassAntigrief: Boolean) = mockk<Minion> {
        every { mining } returns MiningState()
        every { state } returns MinionState(definition = MinionDefinitionFixture.build(bypassAntigrief = bypassAntigrief))
    }

    private var online = true

    private fun protection(allowed: Boolean = true) = MinionProtection(
        builderOf = { if (online) builder else null },
        canBuild = { player, _ -> asked++; askedFor += player; allowed },
        clock = { now }
    )

    @Test
    fun `a block the owner can build at is allowed`() {
        assertTrue(protection(allowed = true).allows(minion, world.blockAt(1, 0, 0)))
    }

    @Test
    fun `a block the owner cannot build at is refused`() {
        assertFalse(protection(allowed = false).allows(minion, world.blockAt(1, 0, 0)))
    }

    /** A work area is scanned every cycle, and protection plugins are slow to ask. */
    @Test
    fun `each block is asked about once`() {
        val protection = protection()

        repeat(3) { protection.allows(minion, world.blockAt(1, 0, 0)) }
        protection.allows(minion, world.blockAt(2, 0, 0))

        assertEquals(2, asked)
    }

    /** A claim made or removed beside a working minion is picked up. */
    @Test
    fun `answers are asked again after a minute`() {
        val protection = protection()
        protection.allows(minion, world.blockAt(1, 0, 0))

        now = 60_000L
        protection.allows(minion, world.blockAt(1, 0, 0))

        assertEquals(2, asked)
    }

    /** With `active-while-member-online`, a minion can run with nobody it can be checked against. */
    @Test
    fun `no builder refuses the block`() {
        online = false

        assertFalse(protection(allowed = true).allows(minion, world.blockAt(1, 0, 0)))
        assertEquals(0, asked)
    }

    @Test
    fun `no builder refusal is not kept`() {
        val protection = protection()
        online = false
        protection.allows(minion, world.blockAt(1, 0, 0))

        online = true

        assertTrue(protection.allows(minion, world.blockAt(1, 0, 0)))
        assertEquals(1, asked)
    }

    @Test
    fun `no builder skips the cache`() {
        val protection = protection(allowed = true)
        protection.allows(minion, world.blockAt(1, 0, 0))

        online = false

        assertFalse(protection.allows(minion, world.blockAt(1, 0, 0)))
    }

    @Test
    fun `a new builder is asked again`() {
        val protection = protection()
        protection.allows(minion, world.blockAt(1, 0, 0))

        builder = member
        protection.allows(minion, world.blockAt(1, 0, 0))

        assertEquals(listOf(owner, member), askedFor)
    }

    @Test
    fun `a type that bypasses antigrief is never refused`() {
        val bypassing = minionOf(bypassAntigrief = true)

        assertTrue(protection(allowed = false).allows(bypassing, world.blockAt(1, 0, 0)))
        assertEquals(0, asked)
    }
}
