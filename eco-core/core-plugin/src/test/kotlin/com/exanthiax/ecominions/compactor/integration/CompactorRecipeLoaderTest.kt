package com.exanthiax.ecominions.compactor.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.ConditionList
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CompactorRecipeLoaderTest {

    private val loader = CompactorRecipeLoader()
    private val noConditions = ConditionList(emptyList())

    private fun context(): Pair<LoadContext, LoadReport> {
        val report = LoadReport()
        return LoadContext(mockk<ViolationContext>(relaxed = true), report) to report
    }

    private fun config(
        input: String? = "COAL",
        output: String? = "COAL_BLOCK",
        inputAmount: Int = 9,
        outputAmount: Int = 1,
        higherLevelOf: String? = null
    ): Config {
        val config = mockk<Config>(relaxed = true)
        every { config.getStringOrNull("input") } returns input
        every { config.getStringOrNull("output") } returns output
        every { config.getInt("input-amount") } returns inputAmount
        every { config.getInt("output-amount") } returns outputAmount
        every { config.getStringOrNull("higher-level-of") } returns higherLevelOf
        return config
    }

    @Test
    fun `a valid recipe is read`() {
        val (context, report) = context()

        val recipe = loader.readRecipe("coal_block", config(), context, noConditions)

        requireNotNull(recipe)
        assertEquals("coal_block", recipe.id)
        assertEquals("COAL", recipe.inputLookup)
        assertEquals(9, recipe.inputAmount)
        assertEquals("COAL_BLOCK", recipe.outputLookup)
        assertEquals(1, recipe.outputAmount)
        assertNull(recipe.higherLevelOf)
        assertEquals(0, report.violationCount)
    }

    @Test
    fun `higher-level-of is read when present`() {
        val (context, _) = context()

        val recipe = loader.readRecipe(
            "enchanted_coal",
            config(higherLevelOf = "coal_block"),
            context,
            noConditions
        )

        assertEquals("coal_block", recipe?.higherLevelOf)
    }

    @Test
    fun `a missing input is rejected and reported`() {
        val (context, report) = context()

        val recipe = loader.readRecipe("broken", config(input = ""), context, noConditions)

        assertNull(recipe)
        assertEquals(1, report.violationCount)
    }

    @Test
    fun `a missing output is rejected and reported`() {
        val (context, report) = context()

        val recipe = loader.readRecipe("broken", config(output = null), context, noConditions)

        assertNull(recipe)
        assertEquals(1, report.violationCount)
    }

    @Test
    fun `a non-positive input amount is rejected`() {
        val (context, report) = context()

        val recipe = loader.readRecipe("broken", config(inputAmount = 0), context, noConditions)

        assertNull(recipe)
        assertEquals(1, report.violationCount)
    }

    @Test
    fun `a non-positive output amount is rejected`() {
        val (context, report) = context()

        val recipe = loader.readRecipe("broken", config(outputAmount = 0), context, noConditions)

        assertNull(recipe)
        assertEquals(1, report.violationCount)
    }
}
