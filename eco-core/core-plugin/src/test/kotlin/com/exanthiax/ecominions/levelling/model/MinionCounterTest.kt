package com.exanthiax.ecominions.levelling.model

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.effects.arguments.EffectArgumentList
import com.willfp.libreforge.filters.FilterList
import com.willfp.libreforge.triggers.DispatchedTrigger
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionCounterTest {

    private val trigger = mockk<Trigger>(relaxed = true)
    private val otherTrigger = mockk<Trigger>(relaxed = true)
    private val config = mockk<Config>(relaxed = true)

    private fun dispatch(
        firing: Trigger = trigger,
        value: Double = 4.0
    ): DispatchedTrigger {
        val data = mockk<TriggerData>(relaxed = true)
        every { data.value } returns value

        val dispatched = mockk<DispatchedTrigger>(relaxed = true)
        every { dispatched.trigger } returns firing
        every { dispatched.data } returns data
        return dispatched
    }

    private fun counter(
        multiplier: String? = null,
        value: String? = null,
        filters: FilterList = FilterList(emptyList()),
        conditions: ConditionList = ConditionList(emptyList())
    ) = MinionCounter(
        trigger = trigger,
        conditions = conditions,
        filters = filters,
        config = config,
        arguments = EffectArgumentList(emptyList()),
        multiplierExpression = multiplier,
        valueExpression = value
    ) { expression, _ -> expression.toDouble() }

    @Test
    fun `a bare counter grants the trigger value`() {
        assertEquals(4.0, counter().amountFor(dispatch()))
    }

    @Test
    fun `a multiplier scales the trigger value`() {
        assertEquals(10.0, counter(multiplier = "2.5").amountFor(dispatch()))
    }

    @Test
    fun `a value expression ignores the trigger value`() {
        assertEquals(7.0, counter(value = "7").amountFor(dispatch(value = 100.0)))
    }

    @Test
    fun `a counter does not apply to a different trigger`() {
        assertNull(counter().amountFor(dispatch(firing = otherTrigger)))
    }

    @Test
    fun `a counter does not apply when a filter rejects`() {
        val rejecting = mockk<FilterList>(relaxed = true)
        every { rejecting.isMet(any()) } returns false

        assertNull(counter(filters = rejecting).amountFor(dispatch()))
    }

    @Test
    fun `a counter does not apply when a condition rejects`() {
        val rejecting = mockk<ConditionList>(relaxed = true)
        every { rejecting.areMetAndTrigger(any()) } returns false

        assertNull(counter(conditions = rejecting).amountFor(dispatch()))
    }
}
