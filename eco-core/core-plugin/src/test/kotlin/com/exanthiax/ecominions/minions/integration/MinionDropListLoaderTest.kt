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
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionDropListLoaderTest {
    private val loader = MinionDropListLoader(
        FakeItemLookup(mapOf("cod" to FakeItemLookup.testable(Material.COD)))
    )

    private fun section(item: String?, chance: Double? = null, amount: String? = null, xp: String? = null): Config =
        mockk(relaxed = true) {
            every { getStringOrNull("item") } returns item
            every { getDoubleOrNull("chance") } returns chance
            every { getStringOrNull("amount") } returns amount
            every { getStringOrNull("xp") } returns xp
        }

    @Test
    fun `reads item, chance, amount and xp`() {
        val entry = loader.load(listOf(section("cod", 0.5, "1-3", "2")), "drops", context()).entries.single()

        assertEquals(Material.COD, entry.item.type)
        assertEquals(0.5, entry.chance)
        assertEquals(1..3, entry.amount)
        assertEquals(2..2, entry.experience)
    }

    @Test
    fun `chance and amount default to one`() {
        val entry = loader.load(listOf(section("cod")), "drops", context()).entries.single()

        assertEquals(1.0, entry.chance)
        assertEquals(1..1, entry.amount)
        assertNull(entry.experience)
    }

    @Test
    fun `an unknown item is reported and skipped`() {
        val violations = mutableListOf<String>()

        assertTrue(loader.load(listOf(section("nope")), "drops", context(violations)).isEmpty)
        assertEquals(listOf("drops"), violations)
    }

    @Test
    fun `a chance above one is reported and clamped`() {
        val violations = mutableListOf<String>()

        assertEquals(1.0, loader.load(listOf(section("cod", chance = 2.0)), "drops", context(violations)).entries.single().chance)
        assertEquals(listOf("drops"), violations)
    }

    @Test
    fun `a bad amount is reported and defaults to one`() {
        val violations = mutableListOf<String>()

        assertEquals(1..1, loader.load(listOf(section("cod", amount = "x")), "drops", context(violations)).entries.single().amount)
        assertEquals(listOf("drops"), violations)
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers { violations += captured.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
