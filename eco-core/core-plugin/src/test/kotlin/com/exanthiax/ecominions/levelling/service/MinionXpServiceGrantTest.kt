package com.exanthiax.ecominions.levelling.service

import com.exanthiax.ecominions.api.event.MinionXpGainEvent
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.progression.LevelCurve
import com.willfp.libreforge.conditions.ConditionList
import io.mockk.every
import io.mockk.mockk
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MinionXpServiceGrantTest {

    private val laddered = MinionDefinitionFixture.build(
        levelling = LevellingSpec(
            enabled = true,
            curve = LevelCurve.Requirements(listOf(100.0, 200.0, 300.0), startLevel = 1, configuredMaxLevel = null),
            gainMethods = emptyList(),
            conditions = ConditionList(emptyList()),
            levelUpEffects = null
        )
    )

    private var state = MinionState(definition = laddered)
    private val minion = mockk<Minion>(relaxed = true) { every { state } answers { this@MinionXpServiceGrantTest.state } }

    private val listener = RegisteredListener(
        object : Listener {},
        EventExecutor { _, _ -> },
        EventPriority.NORMAL,
        mockk<Plugin>(relaxed = true),
        false
    )

    @BeforeEach
    fun listen() {
        MinionXpGainEvent.getHandlerList().register(listener)
    }

    @AfterEach
    fun stopListening() {
        MinionXpGainEvent.getHandlerList().unregister(listener)
    }

    private fun service(events: RecordingEventDispatcher) = MinionXpService(
        applyState = { _, updated -> state = updated },
        onLevelChanged = {},
        events = events
    )

    @Test
    fun `a listener's amount is the one banked`() {
        val events = RecordingEventDispatcher { (it as? MinionXpGainEvent)?.amount = 10.0 }

        assertTrue(service(events).grant(minion, 40.0))

        assertEquals(10.0, state.xp)
    }

    @Test
    fun `a cancelled gain banks nothing`() {
        assertFalse(service(RecordingEventDispatcher(allow = false)).grant(minion, 40.0))

        assertEquals(0.0, state.xp)
    }

    @Test
    fun `a type that cannot level fires nothing`() {
        state = MinionState(definition = MinionDefinitionFixture.build())
        val events = RecordingEventDispatcher()

        assertFalse(service(events).grant(minion, 40.0))

        assertTrue(events.events.isEmpty())
    }

    @Test
    fun `setting a level clears banked xp`() {
        state = state.withXp(50.0, 1)

        assertTrue(service(RecordingEventDispatcher()).setLevel(minion, 3))

        assertEquals(3, state.level)
        assertEquals(0.0, state.xp)
    }

    @Test
    fun `a level past the top is refused`() {
        assertFalse(service(RecordingEventDispatcher()).setLevel(minion, 99))
    }
}
