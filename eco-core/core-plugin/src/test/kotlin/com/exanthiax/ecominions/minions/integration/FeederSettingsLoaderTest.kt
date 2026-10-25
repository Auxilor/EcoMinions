package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FeederSettingsLoaderTest {
    private val coal = Fuel(
        id = "coal",
        displayName = "Coal",
        itemLookup = "coal",
        durationTicks = 1_200L,
        speedMultiplier = 2.0
    )
    private val fuels = mockk<FuelService> {
        every { getById(any()) } returns null
        every { getById("coal") } returns coal
    }
    private val loader = FeederSettingsLoader(fuels)

    @Test
    fun `empty fuels means every fuel`() {
        val config = mockk<Config>(relaxed = true) { every { getStrings("fuels") } returns emptyList() }

        assertEquals(emptyList<String>(), loader.load(config, context())?.fuelIds)
    }

    @Test
    fun `unknown fuel id is dropped`() {
        val violations = mutableListOf<String>()
        val config = mockk<Config>(relaxed = true) { every { getStrings("fuels") } returns listOf("coal", "nope") }

        val settings = loader.load(config, context(violations))

        assertEquals(listOf("coal"), settings?.fuelIds)
        assertEquals(listOf("fuels"), violations)
    }

    @Test
    fun `all fuels invalid rejects`() {
        val violations = mutableListOf<String>()
        val config = mockk<Config>(relaxed = true) { every { getStrings("fuels") } returns listOf("nope", "nada") }

        assertNull(loader.load(config, context(violations)))

        assertEquals(listOf("fuels", "fuels", "fuels"), violations)
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers { violations += captured.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
