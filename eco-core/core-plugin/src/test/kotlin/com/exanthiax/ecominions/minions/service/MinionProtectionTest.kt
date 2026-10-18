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

class MinionProtectionTest {

    private val world = FakeWorld()
    private val owner = mockk<Player>()
    private val minion = minionOf(bypassAntigrief = false)
    private var now = 0L
    private var asked = 0

    private fun minionOf(bypassAntigrief: Boolean) = mockk<Minion> {
        every { mining } returns MiningState()
        every { state } returns MinionState(definition = MinionDefinitionFixture.build(bypassAntigrief = bypassAntigrief))
    }

    private fun protection(allowed: Boolean = true, online: Boolean = true) = MinionProtection(
        ownerOf = { if (online) owner else null },
        canBuild = { _, _ -> asked++; allowed },
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

    /** An offline owner's minion is idle, and an unowned one belongs to nobody to check. */
    @Test
    fun `without an online owner nothing is asked`() {
        assertTrue(protection(allowed = false, online = false).allows(minion, world.blockAt(1, 0, 0)))
        assertEquals(0, asked)
    }

    @Test
    fun `a type that bypasses antigrief is never refused`() {
        val bypassing = minionOf(bypassAntigrief = true)

        assertTrue(protection(allowed = false).allows(bypassing, world.blockAt(1, 0, 0)))
        assertEquals(0, asked)
    }
}
