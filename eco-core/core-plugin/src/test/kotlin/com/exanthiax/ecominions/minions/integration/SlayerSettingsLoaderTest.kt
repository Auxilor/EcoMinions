package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.testsupport.FakeEntityLookup
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.bukkit.entity.EntityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlayerSettingsLoaderTest {
    private val loader = SlayerSettingsLoader(
        FakeEntityLookup(mapOf("zombie" to FakeEntityLookup.testable(EntityType.ZOMBIE))),
        MinionDropListLoader(FakeItemLookup())
    )

    private fun config(mobs: List<String>, maxMobs: String? = null): Config = mockk(relaxed = true) {
        every { getStrings("target-mobs") } returns mobs
        every { getStringOrNull("max-mobs") } returns maxMobs
        every { getBoolOrNull("credit-owner") } returns null
        every { getSubsections("drops") } returns emptyList()
    }

    @Test
    fun `target-mobs is required`() {
        val violations = mutableListOf<String>()

        assertNull(loader.load(config(emptyList()), context(violations)))
        assertEquals(listOf("target-mobs"), violations)
    }

    @Test
    fun `an unknown mob is reported and the rest load`() {
        val violations = mutableListOf<String>()

        val settings = loader.load(config(listOf("zombie", "dragonling")), context(violations))

        assertEquals(1, settings!!.targets.size)
        assertEquals(listOf("target-mobs"), violations)
    }

    @Test
    fun `max-mobs defaults to four`() {
        assertEquals("4", loader.load(config(listOf("zombie")), context())!!.maxMobs)
    }

    @Test
    fun `credit-owner defaults to off`() {
        assertFalse(loader.load(config(listOf("zombie")), context())!!.creditOwner)
    }

    @Test
    fun `credit-owner can be turned on`() {
        val config = config(listOf("zombie"))
        every { config.getBoolOrNull("credit-owner") } returns true

        assertTrue(loader.load(config, context())!!.creditOwner)
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers { violations += captured.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
