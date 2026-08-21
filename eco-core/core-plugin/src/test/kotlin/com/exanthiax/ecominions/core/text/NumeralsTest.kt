package com.exanthiax.ecominions.core.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumeralsTest {

    @Test
    fun `single digits convert`() {
        assertEquals("I", Numerals.roman(1))
        assertEquals("IV", Numerals.roman(4))
        assertEquals("IX", Numerals.roman(9))
    }

    @Test
    fun `compound values convert`() {
        assertEquals("XLII", Numerals.roman(42))
        assertEquals("MCMXCIV", Numerals.roman(1994))
    }

    @Test
    fun `values with no numeral fall back to digits`() {
        assertEquals("0", Numerals.roman(0))
        assertEquals("-3", Numerals.roman(-3))
    }
}
