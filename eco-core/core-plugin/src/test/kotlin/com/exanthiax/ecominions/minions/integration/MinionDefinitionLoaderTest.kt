package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.levelling.integration.LevellingLoader
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
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

    @Test
    fun `rejects a woodcutter aimed at something that is not a tree`() {
        val messages = mutableListOf<String>()

        val loaded = load(
            type = "woodcutter",
            targets = listOf("DIAMOND_BLOCK"),
            blocks = FakeBlockLookup(mapOf("DIAMOND_BLOCK" to FakeBlockLookup.testable(Material.DIAMOND_BLOCK))),
            context = context(messages = messages)
        )

        assertNull(loaded, "a woodcutter would clear this block and never put it back")
        assertTrue(
            messages.single().contains("DIAMOND_BLOCK"),
            "the violation should name the offending block, was: ${messages.single()}"
        )
    }

    @Test
    fun `rejects a woodcutter whose target has a plant but no leaves`() {
        val oak = MinionBlock.regenDefault("oak", "OAK_LOG", FakeBlockLookup.testable(Material.OAK_LOG))
            .copy(plant = FakeBlockLookup.testable(Material.OAK_SAPLING))

        val loaded = load(
            type = "woodcutter",
            targets = listOf("OAK_LOG"),
            blocks = FakeBlockLookup(mapOf("OAK_LOG" to FakeBlockLookup.testable(Material.OAK_LOG))),
            blockDefinition = { oak },
            context = context()
        )

        assertNull(loaded)
    }

    /**
     * A regression guard for the key `blockDefinition` is called with.
     *
     * `target-blocks` names a block id (`OAK_LOG`); the file defining it is
     * `oak.yml`, whose registry id is `oak`. Looking the catalog up by file name
     * finds nothing, every target degrades to a regen default with no plant, and
     * every woodcutter in the plugin is then rejected as "not a tree".
     *
     * The load still throws further down - stat expressions need a server - so
     * this asserts on the violation rather than on the returned definition.
     */
    @Test
    fun `resolves a block definition by its block id, not its file name`() {
        val oak = MinionBlock.regenDefault("oak", "OAK_LOG", FakeBlockLookup.testable(Material.OAK_LOG))
            .copy(
                plant = FakeBlockLookup.testable(Material.OAK_SAPLING),
                leaves = FakeBlockLookup.testable(Material.OAK_LEAVES)
            )
        val messages = mutableListOf<String>()

        runCatching {
            load(
                type = "woodcutter",
                targets = listOf("OAK_LOG"),
                blocks = FakeBlockLookup(mapOf("OAK_LOG" to FakeBlockLookup.testable(Material.OAK_LOG))),
                blockDefinition = { id -> oak.takeIf { id == "OAK_LOG" } },
                context = context(messages = messages)
            )
        }

        assertTrue(
            messages.none { it.contains("is not a tree") },
            "a properly defined oak was not recognised as a tree: $messages"
        )
    }

    /** Negative spacing has no meaning, and silently treating it as 0 would hide the typo. */
    @Test
    fun `rejects a negative spacing`() {
        val violations = mutableListOf<String>()
        val config = config("miner", listOf("COAL_ORE"))
        val placement = mockk<Config>(relaxed = true)
        every { placement.getIntOrNull("spacing") } returns -1
        every { config.getSubsection("placement") } returns placement
        val blocks = FakeBlockLookup(mapOf("COAL_ORE" to FakeBlockLookup.testable(Material.COAL_ORE)))

        val result = runCatching { loader(blocks).load("coal", config, context(violations)) }

        assertTrue("placement.spacing" in violations, "was: $violations, threw: ${result.exceptionOrNull()}")
    }

    private fun load(type: String?, context: LoadContext) =
        loader().load("coal", config(type), context)

    private fun load(
        type: String?,
        targets: List<String>,
        blocks: FakeBlockLookup,
        blockDefinition: (String) -> MinionBlock? = { null },
        context: LoadContext
    ) = loader(blocks, blockDefinition).load("coal", config(type, targets), context)

    private fun loader(
        blocks: FakeBlockLookup = FakeBlockLookup(),
        blockDefinition: (String) -> MinionBlock? = { null }
    ) = MinionDefinitionLoader(
        blocks = blocks,
        blockDefinition = blockDefinition,
        items = FakeItemLookup(),
        particles = ParticleSettingsLoader(),
        levelling = LevellingLoader(),
        autoPricingEnabled = { false },
        hardSlotCap = { 54 },
        compactorRecipe = { null },
        compactorRecipeIds = { emptyList() },
        upgradeIds = { emptyList() }
    )

    private fun config(type: String?, targets: List<String> = emptyList()): Config {
        val config = mockk<Config>(relaxed = true)
        every { config.getStringOrNull("type") } returns type
        every { config.getStrings("target-blocks") } returns targets
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
