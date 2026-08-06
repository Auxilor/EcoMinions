package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DropRollTest {

    @Test
    fun `without fortune the base amount is kept`() {
        assertEquals(3, DropRoll.amount(baseAmount = 3, fortuneMultiplier = 1.0, roll = 0.99))
    }

    @Test
    fun `a fractional quantity rounds up when the roll lands inside it`() {
        assertEquals(2, DropRoll.amount(baseAmount = 1, fortuneMultiplier = 1.5, roll = 0.49))
    }

    @Test
    fun `a fractional quantity rounds down when the roll misses`() {
        assertEquals(1, DropRoll.amount(baseAmount = 1, fortuneMultiplier = 1.5, roll = 0.5))
    }

    @Test
    fun `a whole quantity never rounds up`() {
        assertEquals(2, DropRoll.amount(baseAmount = 1, fortuneMultiplier = 2.0, roll = 0.0))
    }

    @Test
    fun `a zero multiplier drops nothing`() {
        assertEquals(0, DropRoll.amount(baseAmount = 5, fortuneMultiplier = 0.0, roll = 0.0))
    }

    @Test
    fun `fortune scales multi-item drops`() {
        assertEquals(6, DropRoll.amount(baseAmount = 4, fortuneMultiplier = 1.5, roll = 0.9))
    }
}
