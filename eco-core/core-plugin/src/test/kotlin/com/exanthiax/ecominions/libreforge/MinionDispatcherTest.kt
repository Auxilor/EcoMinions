package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.get
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionDispatcherTest {

    @Test
    fun `dispatcher exposes the minion uuid and location`() {
        val id = UUID.randomUUID()
        val location = mockk<Location>()
        val minion = mockk<Minion> {
            every { uuid } returns id
            every { this@mockk.location } returns location
        }

        val dispatcher = minion.toDispatcher()

        assertEquals(id, dispatcher.uuid)
        assertSame(location, dispatcher.location)
    }

    @Test
    fun `dispatcher resolves back to the minion by type`() {
        val minion = mockk<Minion> {
            every { uuid } returns UUID.randomUUID()
            every { location } returns mockk()
        }

        val dispatcher = minion.toDispatcher()

        assertSame(minion, dispatcher.get<Minion>())
    }
}
