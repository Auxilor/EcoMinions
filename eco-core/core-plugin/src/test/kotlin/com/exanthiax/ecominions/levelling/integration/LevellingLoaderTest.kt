package com.exanthiax.ecominions.levelling.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.levelling.model.LevelCurve
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Exercises [LevellingLoader.readCurve] directly rather than [LevellingLoader.load]:
 * `load` also compiles conditions/effects through libreforge's registries, which
 * require a live plugin instance and cannot run in a plain unit test.
 */
class LevellingLoaderTest {

    private val loader = LevellingLoader()

    private fun section(maxLevel: Int?, requirements: List<Double>?, formula: String? = null): Config {
        val section = mockk<Config>(relaxed = true)
        every { section.getIntOrNull("max-level") } returns maxLevel
        every { section.getStringOrNull("xp-formula") } returns formula
        every { section.getDoublesOrNull("xp-requirements") } returns requirements
        return section
    }

    private fun context(): Pair<LoadContext, LoadReport> {
        val report = LoadReport()
        return LoadContext(mockk<ViolationContext>(relaxed = true), report) to report
    }

    @Test
    fun `max-level above the requirements ceiling is clamped and reported`() {
        val (context, report) = context()

        val curve = loader.readCurve(section(maxLevel = 50, requirements = listOf(100.0)), context)

        assertEquals(2, (curve as LevelCurve.Requirements).maxLevel)
        assertEquals(1, report.violationCount)
    }

    @Test
    fun `max-level within the requirements ceiling is left alone`() {
        val (context, report) = context()

        val curve = loader.readCurve(
            section(maxLevel = 2, requirements = listOf(100.0, 200.0)),
            context
        )

        assertEquals(2, (curve as LevelCurve.Requirements).maxLevel)
        assertEquals(0, report.violationCount)
    }

    @Test
    fun `no explicit max-level is untouched and unreported`() {
        val (context, report) = context()

        val curve = loader.readCurve(section(maxLevel = null, requirements = listOf(100.0)), context)

        assertEquals(2, (curve as LevelCurve.Requirements).maxLevel)
        assertEquals(0, report.violationCount)
    }

    @Test
    fun `a formula curve has no natural ceiling to clamp against`() {
        val (context, report) = context()

        val curve = loader.readCurve(
            section(maxLevel = 500, requirements = null, formula = "100 * %level%"),
            context
        )

        assertEquals(500, (curve as LevelCurve.Formula).maxLevel)
        assertEquals(0, report.violationCount)
    }

    @Test
    fun `neither a formula nor requirements yields no curve`() {
        val (context, _) = context()

        val curve = loader.readCurve(section(maxLevel = null, requirements = null), context)

        assertNull(curve)
    }
}
