package com.exanthiax.ecominions.core.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IntRangesTest {
    @Test
    fun `a single number is a one value range`() {
        assertEquals(3..3, IntRanges.parse("3"))
    }

    @Test
    fun `a dash range is read`() {
        assertEquals(2..5, IntRanges.parse(" 2-5 "))
    }

    @Test
    fun `reversed, negative and junk values are rejected`() {
        assertNull(IntRanges.parse("5-2"))
        assertNull(IntRanges.parse("-1"))
        assertNull(IntRanges.parse("abc"))
        assertNull(IntRanges.parse("1-2-3"))
    }

    @Test
    fun `blank is no range`() {
        assertNull(IntRanges.parse(""))
        assertNull(IntRanges.parse(null))
    }
}
