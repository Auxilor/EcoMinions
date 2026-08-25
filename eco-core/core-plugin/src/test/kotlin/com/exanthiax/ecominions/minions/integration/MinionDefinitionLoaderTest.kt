package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.levelling.integration.LevellingLoader
import com.exanthiax.ecominions.minions.model.GameData
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers the `type:` gate only.
 *
 * A successful load runs the whole loader, which evaluates `%level%` stat
 * expressions through eco's expression engine and so needs a server. The gate
 * rejects before reaching any of that, which is why these three cases are
 * testable here and a happy path is not.
 */
class MinionDefinitionLoaderTest {

    @Test
    fun `rejects a minion with no type`() {
        val violations = mutableListOf<String>()

        assertNull(load(type = null, context = context(violations)))
        assertEquals(listOf("type"), violations)
    }

    @Test
    fun `rejects a blank type`() {
        val violations = mutableListOf<String>()

        assertNull(load(type = "   ", context = context(violations)))
        assertEquals(listOf("type"), violations)
    }

    @Test
    fun `rejects an unknown type and lists what would have worked`() {
        val messages = mutableListOf<String>()

        assertNull(load(type = "wizard", context = context(messages = messages)))
        assertTrue(
            messages.single().contains("miner"),
            "the violation should list the valid types, was: ${messages.single()}"
        )
    }

    private fun load(type: String?, context: LoadContext) =
        loader().load("coal", config(type), context)

    private fun loader() = MinionDefinitionLoader(
        gameData = GameData.EMPTY,
        blocks = FakeBlockLookup(),
        blockDefinition = { null },
        items = FakeItemLookup(),
        particles = ParticleSettingsLoader(),
        levelling = LevellingLoader(),
        autoPricingEnabled = { false },
        hardSlotCap = { 54 },
        compactorRecipe = { null },
        compactorRecipeIds = { emptyList() }
    )

    private fun config(type: String?): Config {
        val config = mockk<Config>(relaxed = true)
        every { config.getStringOrNull("type") } returns type
        return config
    }

    private fun context(
        violations: MutableList<String> = mutableListOf(),
        messages: MutableList<String> = mutableListOf()
    ): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val captured = slot<ConfigViolation>()
        every { violationContext.log(capture(captured)) } answers {
            violations += captured.captured.param
            messages += captured.captured.message
        }
        return LoadContext(violationContext, LoadReport())
    }
}
