package com.exanthiax.ecominions.minions.integration

import com.willfp.eco.core.config.interfaces.Config
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionSettingsLoaderTest {
    @Test
    fun `missing island keys fall back to the defaults`() {
        val config = mockk<Config>(relaxed = true) {
            every { getString("minion.regen-marker-fallback-block") } returns "air"
            every { getBoolOrNull(any()) } returns null
            every { getIntOrNull(any()) } returns null
        }

        val settings = MinionSettingsLoader(config).load()

        assertTrue(settings.islandPrivileges)
        assertFalse(settings.activeWhileMemberOnline)
        assertFalse(settings.islandLimitsEnabled)
        assertEquals(10, settings.defaultIslandLimit)
        assertTrue(settings.islandPermissionOverrides)
    }
}
