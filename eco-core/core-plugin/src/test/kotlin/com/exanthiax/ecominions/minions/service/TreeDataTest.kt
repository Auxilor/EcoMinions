package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.data.JsonData
import com.exanthiax.ecominions.minions.model.TreeShape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the shipped `trees.json` itself.
 *
 * Its predecessor shipped an empty `by-family` map for long enough that every
 * species grew the same stub, and nothing failed - the lookup just missed and
 * fell back to the default. Only a test over the data catches that.
 */
class TreeDataTest {

    private val root = JsonData.read("/data/trees.json")
    private val bySpecies = root.getAsJsonObject("by-species")

    /** Every species [com.exanthiax.ecominions.minions.integration.VanillaBlockDefaults] knows about. */
    private val vanillaTrees = listOf(
        "oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "cherry", "pale_oak", "mangrove"
    )

    @Test
    fun `every vanilla species has its own shape`() {
        val missing = vanillaTrees.filterNot { bySpecies.has(it) }

        assertTrue(missing.isEmpty(), "these species would silently grow the generic shape: $missing")
    }

    @Test
    fun `every shape names a style that exists`() {
        val styles = TreeShape.Style.entries.map { it.name.lowercase() }

        for ((id, value) in bySpecies.entrySet()) {
            val style = value.asJsonObject["style"].asString.lowercase()
            assertTrue(style in styles, "'$id' names style '$style', which is not one of $styles")
        }
    }

    @Test
    fun `no species grows a trunk its own minion cannot reach`() {
        for ((id, value) in bySpecies.entrySet()) {
            val trunk = value.asJsonObject["trunk"].asInt
            assertTrue(
                trunk <= TreeShape.MAX_REACHABLE_TRUNK,
                "'$id' grows a $trunk-log trunk, which a minion on the default height cannot fell"
            )
        }
    }

    @Test
    fun `the species are not all the same tree`() {
        val distinct = bySpecies.entrySet().map { (_, value) ->
            val o = value.asJsonObject
            listOf(o["trunk"].asInt, o["radius"].asInt, o["layers"].asInt, o["style"].asString)
        }.toSet()

        assertTrue(distinct.size > 1, "every species resolves to one shape - this is the empty by-family bug")
    }

    @Test
    fun `the default shape is present and readable`() {
        val default = root.getAsJsonObject("default")

        assertEquals(TreeShape.MAX_REACHABLE_TRUNK, default["trunk"].asInt)
    }
}
