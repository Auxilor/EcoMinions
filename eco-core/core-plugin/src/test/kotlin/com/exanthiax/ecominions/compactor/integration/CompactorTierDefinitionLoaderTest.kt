package com.exanthiax.ecominions.compactor.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompactorTierDefinitionLoaderTest {

    private val loader = CompactorTierDefinitionLoader()

    private fun context(): Pair<LoadContext, LoadReport> {
        val report = LoadReport()
        return LoadContext(mockk<ViolationContext>(relaxed = true), report) to report
    }

    private fun recipesSection(vararg entries: Triple<String, String, String>): Config {
        val root = mockk<Config>(relaxed = true)
        val recipes = mockk<Config>(relaxed = true)
        every { root.getSubsection("recipes") } returns recipes
        every { recipes.getKeys(false) } returns entries.map { it.first }

        for ((key, input, output) in entries) {
            val entry = mockk<Config>(relaxed = true)
            every { entry.getStringOrNull("input") } returns input
            every { entry.getStringOrNull("output") } returns output
            every { entry.getInt("input-amount") } returns 9
            every { entry.getInt("output-amount") } returns 1
            every { recipes.getSubsection(key) } returns entry
        }
        return root
    }

    @Test
    fun `every recipe in the tier is read`() {
        val (context, report) = context()

        val recipes = loader.readRecipes(
            "basic_block",
            recipesSection(Triple("coal_block", "COAL", "COAL_BLOCK"), Triple("iron_block", "IRON_INGOT", "IRON_BLOCK")),
            context
        )

        assertEquals(2, recipes.size)
        assertEquals(0, report.violationCount)
    }

    @Test
    fun `a recipe missing an input is dropped and reported`() {
        val (context, report) = context()

        val recipes = loader.readRecipes(
            "basic_block",
            recipesSection(Triple("broken", "", "COAL_BLOCK")),
            context
        )

        assertTrue(recipes.isEmpty())
        assertEquals(1, report.violationCount)
    }
}
