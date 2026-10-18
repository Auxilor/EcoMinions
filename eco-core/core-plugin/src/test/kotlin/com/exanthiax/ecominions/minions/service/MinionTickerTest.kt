package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionStorageFullEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.activity.ActivityResult
import com.exanthiax.ecominions.minions.service.activity.MinionActivity
import com.exanthiax.ecominions.minions.service.activity.OfflineYield
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionTickerTest {

    private val stats = MinionStats(
        MinionDefinition.BaseStats(
            speedTicks = "20",
            efficiencyTicks = "5",
            radius = "2",
            storageSlots = "1",
            maxStorageSlots = "54",
            hardSlotCap = 54,
            regenDelayTicks = "60"
        ),
        level = 1
    )

    private val fuels = mockk<FuelService> {
        every { activeFuelOf(any()) } returns null
        every { effectiveSpeedTicks(any(), any()) } answers { secondArg() }
    }

    private val worlds = mockk<WorldSettings> { every { isDisabled(any()) } returns false }

    private class ScriptedActivity(private val result: ActivityResult) : MinionActivity {
        var acted = 0
        var ticked = 0

        override fun onTick(minion: Minion, now: Long) {
            ticked++
        }

        override fun act(minion: Minion, now: Long): ActivityResult {
            acted++
            minion.blockedReason = "scripted"
            return result
        }

        override fun hasWork(minion: Minion): Boolean = true

        override fun offlineCycle(minion: Minion): OfflineYield? = null
    }

    private fun minion(): Minion = Minion(
        state = MinionState(definition = MinionDefinitionFixture.build()),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(1) { a, b -> a.type == b.type },
        stats = stats
    )

    private fun ticker(
        activity: MinionActivity,
        events: EventDispatcher = RecordingEventDispatcher(),
        updateConditions: (Minion) -> Unit = {}
    ) = MinionTicker(
        worlds = worlds,
        fuels = fuels,
        dispatch = mockk(relaxed = true),
        events = events,
        activityOf = { activity },
        dueTriggers = { emptyList() },
        updateConditions = updateConditions
    )

    @Test
    fun `conditions are re-evaluated every 20 ticks`() {
        val subject = minion()
        var polls = 0
        val ticker = ticker(ScriptedActivity(ActivityResult.WORKED), updateConditions = { polls++ })

        for (tick in 100L..140L) {
            ticker.tick(subject, tick)
        }

        assertEquals(3, polls)
    }

    @Test
    fun `worked sets mining animation and waits speed plus efficiency`() {
        val subject = minion()

        ticker(ScriptedActivity(ActivityResult.WORKED)).tick(subject, 100L)

        assertEquals(Minion.AnimationState.MINING, subject.animation)
        assertEquals(125L, subject.mining.nextActionAt)
        assertFalse(subject.mining.seededLastCycle)
    }

    @Test
    fun `seeded idles and marks the cycle as seeded`() {
        val subject = minion()

        ticker(ScriptedActivity(ActivityResult.SEEDED)).tick(subject, 100L)

        assertEquals(Minion.AnimationState.IDLE, subject.animation)
        assertEquals(120L, subject.mining.nextActionAt)
        assertTrue(subject.mining.seededLastCycle)
    }

    @Test
    fun `busy idles and clears the seeded mark`() {
        val subject = minion()
        subject.mining.seededLastCycle = true

        ticker(ScriptedActivity(ActivityResult.BUSY)).tick(subject, 100L)

        assertEquals(Minion.AnimationState.IDLE, subject.animation)
        assertFalse(subject.mining.seededLastCycle)
    }

    @Test
    fun `blocked shows blocked`() {
        val subject = minion()

        ticker(ScriptedActivity(ActivityResult.BLOCKED)).tick(subject, 100L)

        assertEquals(Minion.AnimationState.BLOCKED, subject.animation)
        assertEquals(120L, subject.mining.nextActionAt)
    }

    @Test
    fun `full storage skips the activity and announces once`() {
        val subject = minion()
        subject.storage.add(ItemStack(Material.STONE))
        val activity = ScriptedActivity(ActivityResult.WORKED)
        val events = RecordingEventDispatcher()
        val ticker = ticker(activity, events)

        ticker.tick(subject, 100L)
        ticker.tick(subject, 120L)

        assertEquals(0, activity.acted)
        assertEquals(1, events.events.filterIsInstance<MinionStorageFullEvent>().size)
        assertEquals(Minion.AnimationState.FULL, subject.animation)
        assertEquals(140L, subject.mining.nextActionAt)
    }

    @Test
    fun `before the next action only onTick runs`() {
        val subject = minion()
        subject.mining.nextActionAt = 200L
        val activity = ScriptedActivity(ActivityResult.WORKED)

        ticker(activity).tick(subject, 100L)

        assertEquals(1, activity.ticked)
        assertEquals(0, activity.acted)
    }

    @Test
    fun `the blocked reason is reset before each action`() {
        val subject = minion()
        subject.blockedReason = "no-water"

        ticker(ScriptedActivity(ActivityResult.WORKED)).tick(subject, 100L)

        assertEquals("scripted", subject.blockedReason)
    }

    @Test
    fun `a dead stand does nothing`() {
        val stand = mockk<ArmorStand>(relaxed = true) { every { isDead } returns true }
        val subject = Minion(MinionState(definition = MinionDefinitionFixture.build()), stand, MinionStorage(1), stats)
        val activity = ScriptedActivity(ActivityResult.WORKED)

        ticker(activity).tick(subject, 100L)

        assertEquals(0, activity.ticked)
        assertEquals(0, activity.acted)
    }
}
