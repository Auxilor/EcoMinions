package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.libreforge.MinionDrops
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.FishingSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionDropList
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.FishingLootRoller
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.testsupport.FakeWorld
import com.willfp.libreforge.triggers.event.DropResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class FishingActivityTest {

    private val world = FakeWorld()
    private val sink = mockk<ExperienceSink>(relaxed = true)
    private val roller = mockk<FishingLootRoller> {
        every { roll(any(), any(), any()) } returns OfflineYield(listOf(ItemStack(Material.COD)), 2)
    }
    private val baseStats = MinionDefinition.BaseStats(
        speedTicks = "20",
        efficiencyTicks = "5",
        radius = "1",
        height = "1",
        floorOffset = -1,
        storageSlots = "3",
        maxStorageSlots = "54",
        hardSlotCap = 54,
        regenDelayTicks = "60"
    )

    private fun minion(minWater: Int = 1, selfSeed: Boolean = false) = Minion(
        state = MinionState(
            uuid = UUID.randomUUID(),
            definition = MinionDefinitionFixture.build(
                type = MinionType.FISHING,
                baseStats = baseStats,
                placement = PlacementSettings.NONE.copy(selfSeed = selfSeed),
                fishing = FishingSettings(minWater, MinionDropList.EMPTY)
            )
        ),
        entity = world.stand(),
        storage = MinionStorage(3) { a, b -> a.type == b.type },
        stats = MinionStats(baseStats, 1)
    )

    private fun dispatch(minion: Minion) = mockk<MinionTriggerDispatch>(relaxed = true) {
        every { drops(minion, any(), any(), any(), any(), any(), any(), any()) } answers {
            MinionDrops(thirdArg<List<ItemStack>>().map { DropResult(it, 0) }, 0)
        }
        every { storeAndDispatchCollect(minion, any()) } answers { minion.storage.store(secondArg()) }
    }

    private fun activity(
        minion: Minion,
        events: EventDispatcher = RecordingEventDispatcher(),
        canWork: (Minion, Block) -> Boolean = { _, _ -> true }
    ) = FishingActivity(
        roller = roller,
        dispatch = dispatch(minion),
        events = events,
        experience = sink,
        ownerOf = { null },
        listening = { true },
        isWaterSource = { it.type == Material.WATER },
        splash = {},
        random = Random(1),
        canWork = canWork
    )

    @Test
    fun `self-seed lays no water where the owner cannot build`() {
        world.fillLayer(-2)
        val subject = minion(selfSeed = true)

        activity(subject, canWork = { _, _ -> false }).act(subject, 0L)

        assertEquals(0, world.countOf(Material.WATER))
    }

    @Test
    fun `without enough water it is blocked with no-water`() {
        val subject = minion(minWater = 2)
        world.set(1, -1, 0, Material.WATER)

        assertEquals(ActivityResult.BLOCKED, activity(subject).act(subject, 0L))
        assertEquals("no-water", subject.blockedReason)
    }

    @Test
    fun `with water it casts and catches after the windup`() {
        val subject = minion()
        world.set(1, -1, 0, Material.WATER)
        val activity = activity(subject)

        assertEquals(ActivityResult.WORKED, activity.act(subject, 0L))
        activity.onTick(subject, 5L)

        assertEquals(1, subject.storage.count { it.type == Material.COD })
        verify { sink.add(subject, 2) }
    }

    @Test
    fun `water is rescanned every 20 cycles`() {
        val subject = minion()
        world.set(1, -1, 0, Material.WATER)
        val activity = activity(subject)
        activity.act(subject, 0L)
        world.set(1, -1, 0, Material.AIR)

        repeat(19) { assertEquals(ActivityResult.WORKED, activity.act(subject, 0L)) }

        assertEquals(ActivityResult.BLOCKED, activity.act(subject, 0L))
    }

    @Test
    fun `water placed while blocked is found on the next cycle`() {
        val subject = minion()
        val activity = activity(subject)
        assertEquals(ActivityResult.BLOCKED, activity.act(subject, 0L))

        world.set(1, -1, 0, Material.WATER)

        assertEquals(ActivityResult.WORKED, activity.act(subject, 0L))
    }

    @Test
    fun `self-seed lays one water source on solid ground`() {
        world.fillLayer(-2)
        val subject = minion(selfSeed = true)

        assertEquals(ActivityResult.SEEDED, activity(subject).act(subject, 0L))

        assertEquals(1, world.countOf(Material.WATER))
        assertEquals(1, subject.mining.seeded.size)
    }

    @Test
    fun `a cancelled catch stores nothing`() {
        val subject = minion()
        world.set(1, -1, 0, Material.WATER)
        val activity = activity(subject, RecordingEventDispatcher(allow = false))
        activity.act(subject, 0L)

        activity.onTick(subject, 5L)

        assertEquals(0, subject.storage.count { true })
    }

    @Test
    fun `offline needs enough water`() {
        val subject = minion(minWater = 2)
        world.set(1, -1, 0, Material.WATER)

        assertNull(activity(subject).offlineCycle(subject))
    }
}
