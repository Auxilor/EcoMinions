package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.minions.model.RestoreMode
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.ViolationContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionBlockLoaderTest {

    private val lookup = FakeBlockLookup(
        mapOf(
            "OAK_LOG" to FakeBlockLookup.testable(Material.OAK_LOG),
            "OAK_SAPLING" to FakeBlockLookup.testable(Material.OAK_SAPLING),
            "OAK_LEAVES" to FakeBlockLookup.testable(Material.OAK_LEAVES),
            "GRASS_BLOCK" to FakeBlockLookup.testable(Material.GRASS_BLOCK)
        )
    )

    @Test
    fun `loads a tree definition`() {
        val config = config(
            block = "OAK_LOG",
            restore = "tree",
            plant = "OAK_SAPLING",
            leaves = "OAK_LEAVES",
            surface = "GRASS_BLOCK"
        )

        val loaded = MinionBlockLoader(lookup).load("oak", config, context())

        assertNotNull(loaded)
        assertEquals("OAK_LOG", loaded!!.lookup)
        assertEquals(RestoreMode.TREE, loaded.restore)
        assertNotNull(loaded.plant)
        assertNotNull(loaded.leaves)
        assertNotNull(loaded.surface)
    }

    @Test
    fun `a tree without leaves degrades to regen and is reported`() {
        val violations = mutableListOf<String>()
        val config = config(block = "OAK_LOG", restore = "tree", plant = "OAK_SAPLING")

        val loaded = MinionBlockLoader(lookup).load("oak", config, context(violations))

        assertEquals(RestoreMode.REGEN, loaded!!.restore)
        assertEquals(1, violations.size)
    }

    @Test
    fun `an unresolvable block is rejected`() {
        val violations = mutableListOf<String>()
        val config = config(block = "NOT_A_BLOCK", restore = "regen")

        assertNull(MinionBlockLoader(lookup).load("nope", config, context(violations)))
        assertEquals(1, violations.size)
    }

    @Test
    fun `a missing restore defaults to regen`() {
        val loaded = MinionBlockLoader(lookup).load("oak", config(block = "OAK_LOG"), context())

        assertEquals(RestoreMode.REGEN, loaded!!.restore)
    }

    @Test
    fun `an unknown restore mode is reported and degrades to regen`() {
        val violations = mutableListOf<String>()
        val config = config(block = "OAK_LOG", restore = "explode")

        val loaded = MinionBlockLoader(lookup).load("oak", config, context(violations))

        assertEquals(RestoreMode.REGEN, loaded!!.restore)
        assertEquals(1, violations.size)
    }

    private fun config(
        block: String,
        restore: String? = null,
        plant: String? = null,
        leaves: String? = null,
        surface: String? = null,
        pattern: String? = null,
        drops: List<String> = emptyList()
    ): Config {
        val config = mockk<Config>()
        every { config.getStringOrNull("block") } returns block
        every { config.getStringOrNull("restore") } returns restore
        every { config.getStringOrNull("plant") } returns plant
        every { config.getStringOrNull("leaves") } returns leaves
        every { config.getStringOrNull("surface") } returns surface
        every { config.getStringOrNull("tree-pattern") } returns pattern
        every { config.getStrings("drops") } returns drops
        return config
    }

    private fun context(violations: MutableList<String> = mutableListOf()): LoadContext {
        val violationContext = mockk<ViolationContext>(relaxed = true)
        val param = slot<ConfigViolation>()
        every { violationContext.log(capture(param)) } answers { violations += param.captured.param }
        return LoadContext(violationContext, LoadReport())
    }
}
