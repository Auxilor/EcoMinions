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
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BreederSettingsLoaderTest {
    private val loader = BreederSettingsLoader(
        FakeEntityLookup(mapOf("cow" to FakeEntityLookup.testable(EntityType.COW))),
        FakeItemLookup(mapOf("wheat" to FakeItemLookup.testable(Material.WHEAT)))
    )

    private fun config(
        mobs: List<String> = listOf("cow"),
        food: List<String> = listOf("wheat"),
        maxMobs: String? = null
    ): Config =
        mockk(relaxed = true) {
            every { getStrings("target-mobs") } returns mobs
            every { getStrings("food") } returns food
            every { getStringOrNull("max-mobs") } returns maxMobs
        }

    @Test
    fun `missing food rejects the minion`() {
        val violations = mutableListOf<String>()

        assertNull(loader.load(config(food = emptyList()), context(violations)))
        assertEquals(listOf("food"), violations)
    }

    @Test
    fun `missing target-mobs rejects the minion`() {
        val violations = mutableListOf<String>()

        assertNull(loader.load(config(mobs = emptyList()), context(violations)))
        assertEquals(listOf("target-mobs"), violations)
    }

    @Test
    fun `max-mobs defaults to 8`() {
        assertEquals("8", loader.load(config(), context())!!.maxMobs)
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers { violations += captured.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
