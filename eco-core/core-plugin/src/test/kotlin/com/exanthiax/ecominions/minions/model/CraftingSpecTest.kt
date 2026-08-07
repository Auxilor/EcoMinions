package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CraftingSpecTest {

    @Test
    fun `a full grid is kept as written`() {
        val entries = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i")

        assertEquals(entries, CraftingSpec.normalise(entries))
    }

    @Test
    fun `a short grid is padded with empty slots`() {
        val normalised = CraftingSpec.normalise(listOf("", "stone", ""))

        assertEquals(9, normalised?.size)
        assertEquals(listOf("", "stone", "", "", "", "", "", "", ""), normalised)
    }

    @Test
    fun `an oversized grid is rejected`() {
        assertNull(CraftingSpec.normalise(List(10) { "stone" }))
    }

    @Test
    fun `an entirely empty grid is rejected`() {
        assertNull(CraftingSpec.normalise(List(9) { " " }))
    }

    @Test
    fun `an empty list is rejected`() {
        assertNull(CraftingSpec.normalise(emptyList()))
    }
}
