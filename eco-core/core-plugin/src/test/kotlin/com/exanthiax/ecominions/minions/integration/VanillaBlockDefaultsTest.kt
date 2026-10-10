package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VanillaBlockDefaultsTest {
    private val defaults = VanillaBlockDefaults(
        FakeBlockLookup(
            mapOf(
                "COAL_ORE" to FakeBlockLookup.testable(Material.COAL_ORE),
                "DEEPSLATE_DIAMOND_ORE" to FakeBlockLookup.testable(Material.DEEPSLATE_DIAMOND_ORE),
                "STONE" to FakeBlockLookup.testable(Material.STONE)
            )
        )
    )

    @Test
    fun `ores carry vanilla xp`() {
        assertEquals(0..2, defaults.forId("COAL_ORE")?.experience)
        assertEquals(3..7, defaults.forId("DEEPSLATE_DIAMOND_ORE")?.experience)
    }

    @Test
    fun `stone has no built in definition`() {
        assertNull(defaults.forId("STONE"))
    }
}
