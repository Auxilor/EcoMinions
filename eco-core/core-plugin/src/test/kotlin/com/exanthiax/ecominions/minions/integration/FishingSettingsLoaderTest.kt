package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FishingSettingsLoaderTest {
    private val loader = FishingSettingsLoader(MinionDropListLoader(FakeItemLookup()))

    @Test
    fun `defaults need one water block and have no drops`() {
        val settings = loader.load(mockk<Config>(relaxed = true) { every { getIntOrNull("min-water") } returns null; every { getSubsections("drops") } returns emptyList() }, context())

        assertEquals(1, settings.minWater)
        assertTrue(settings.drops.isEmpty)
    }

    @Test
    fun `min-water below one is reported and raised`() {
        val violations = mutableListOf<String>()

        val settings = loader.load(mockk<Config>(relaxed = true) { every { getIntOrNull("min-water") } returns 0; every { getSubsections("drops") } returns emptyList() }, context(violations))

        assertEquals(1, settings.minWater)
        assertEquals(listOf("min-water"), violations)
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers { violations += captured.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
