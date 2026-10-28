package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.model.OverLimitMode
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

    private fun config(overLimit: String? = null, types: Map<String, Int> = emptyMap()) = mockk<Config>(relaxed = true) {
        every { getString("minion.regen-marker-fallback-block") } returns "air"
        every { getBoolOrNull(any()) } returns null
        every { getIntOrNull(any()) } returns null
        every { getStringOrNull("limits.over-limit") } returns overLimit
        every { getBoolOrNull("limits.per-type.enabled") } returns types.isNotEmpty()
        every { getSubsectionOrNull("limits.per-type.types") } returns mockk<Config> {
            every { getKeys(false) } returns types.keys.toList()
            types.forEach { (key, value) -> every { getInt(key) } returns value }
        }
    }

    @Test
    fun `over-limit defaults to prevent and type limits to off`() {
        val settings = MinionSettingsLoader(config()).load()

        assertEquals(OverLimitMode.PREVENT, settings.overLimit)
        assertFalse(settings.typeLimitsEnabled)
        assertTrue(settings.typeLimitOverrides)
        assertEquals(emptyMap<MinionType, Int>(), settings.typeLimits)
    }

    @Test
    fun `over-limit reads inactive in any case`() {
        assertEquals(OverLimitMode.INACTIVE, MinionSettingsLoader(config(" Inactive ")).load().overLimit)
    }

    @Test
    fun `unknown over-limit falls back to prevent with a warning`() {
        val warnings = mutableListOf<String>()

        val settings = MinionSettingsLoader(config("sometimes"), warnings::add).load()

        assertEquals(OverLimitMode.PREVENT, settings.overLimit)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `type limits read per type and skip unknown types`() {
        val settings = MinionSettingsLoader(config(types = mapOf("miner" to 10, "farmer" to 4, "nonsense" to 2))).load()

        assertTrue(settings.typeLimitsEnabled)
        assertEquals(mapOf(MinionType.MINER to 10, MinionType.FARMER to 4), settings.typeLimits)
    }

    @Test
    fun `rename length defaults to 32`() {
        assertEquals(32, MinionSettingsLoader(config()).load().renameMaxLength)
    }

    @Test
    fun `rename length reads config`() {
        val config = config()
        every { config.getIntOrNull("rename.max-length") } returns 16

        assertEquals(16, MinionSettingsLoader(config).load().renameMaxLength)
    }
}
