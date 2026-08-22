package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.compactor.model.CompactorTierDefinition
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.levelling.integration.LevellingLoader
import com.exanthiax.ecominions.minions.model.GameData
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompactorLadderTest {

    private fun definition(id: String) = CompactorTierDefinition(
        id = id,
        costs = emptyList(),
        recipes = listOf(
            CompactorRecipe(
                id = "${id}_recipe",
                inputLookup = "COAL",
                inputAmount = 9,
                outputLookup = "COAL_BLOCK",
                outputAmount = 1
            )
        )
    )

    private val catalog = mapOf("basic_block" to definition("basic_block"), "enchanted" to definition("enchanted"))

    private val report = LoadReport()

    private fun loader() = MinionTypeLoader(
        gameData = mockk<GameData>(relaxed = true),
        particles = mockk<ParticleSettingsLoader>(relaxed = true),
        levelling = mockk<LevellingLoader>(relaxed = true),
        autoPricingEnabled = { false },
        hardSlotCap = { 54 },
        compactorTier = { catalog[it] },
        compactorTierIds = { catalog.keys }
    )

    private fun config(vararg tierIds: String): Config =
        mockk<Config>(relaxed = true) { every { getStrings("compactor-tiers") } returns tierIds.toList() }

    private fun context() = LoadContext(mockk<ViolationContext>(relaxed = true), report)

    @Test
    fun `a type with no compactor-tiers gets no ladder`() {
        val ladder = loader().readCompactorLadder("coal", config(), context())

        assertNull(ladder.upgrade)
        assertTrue(ladder.recipesByTier.isEmpty())
    }

    @Test
    fun `position in the list becomes the tier level`() {
        val ladder = loader().readCompactorLadder("coal", config("enchanted", "basic_block"), context())

        assertEquals("compactor_coal", ladder.upgrade?.id)
        assertEquals(2, ladder.upgrade?.maxTier)
        assertEquals("enchanted_recipe", ladder.recipesByTier.getValue(1).single().id)
        assertEquals("basic_block_recipe", ladder.recipesByTier.getValue(2).single().id)
        assertTrue(ladder.upgrade!!.selectable)
    }

    @Test
    fun `an unresolvable id truncates the ladder rather than leaving a gap`() {
        val ladder = loader().readCompactorLadder("coal", config("basic_block", "typo", "enchanted"), context())

        assertEquals(1, ladder.upgrade?.maxTier)
        assertEquals(setOf(1), ladder.recipesByTier.keys)
        assertEquals(2, report.violationCount)
    }

    @Test
    fun `a ladder whose first id is unresolvable is no ladder at all`() {
        val ladder = loader().readCompactorLadder("coal", config("typo"), context())

        assertNull(ladder.upgrade)
    }
}
