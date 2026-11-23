package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import wiki.ecohub.config.Config
import wiki.ecohub.config.ConfigViolation
import wiki.ecohub.config.ViolationContext

class CollectionSettingsLoaderTest {
    private val loader = CollectionSettingsLoader(FakeItemLookup(mapOf("cobblestone" to FakeItemLookup.testable(Material.COBBLESTONE))))

    @Test
    fun `defaults collect everything including xp`() {
        val config = mockk<Config>(relaxed = true) { every { getBoolOrNull("collect-xp") } returns null }
        val settings = loader.load(config, context())

        assertTrue(settings.targetItems.isEmpty())
        assertTrue(settings.collectExperience)
    }

    @Test
    fun `an unknown target item is reported and skipped`() {
        val violations = mutableListOf<String>()
        val config = mockk<Config>(relaxed = true) {
            every { getStrings("target-items") } returns listOf("cobblestone", "nope")
            every { getBoolOrNull("collect-xp") } returns false
        }

        val settings = loader.load(config, context(violations))

        assertEquals(1, settings.targetItems.size)
        assertFalse(settings.collectExperience)
        assertEquals(listOf("target-items"), violations)
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers { violations += captured.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
