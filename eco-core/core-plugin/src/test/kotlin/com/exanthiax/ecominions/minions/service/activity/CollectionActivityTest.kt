package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.CollectionSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CollectionActivityTest {

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

    private val items = mutableListOf<Item>()
    private val orbs = mutableListOf<ExperienceOrb>()
    private val entities = object : WorkAreaEntities {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Entity> find(minion: Minion, type: Class<T>): List<T> = when (type) {
            Item::class.java -> items as List<T>
            ExperienceOrb::class.java -> orbs as List<T>
            else -> emptyList()
        }
    }
    private val sink = mockk<ExperienceSink>(relaxed = true)

    private fun minion(settings: CollectionSettings = CollectionSettings(emptyList(), true), slots: Int = 1) = Minion(
        state = MinionState(definition = MinionDefinitionFixture.build(type = MinionType.COLLECTOR, collection = settings)),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(slots) { a, b -> a.type == b.type },
        stats = stats
    )

    private fun dispatch(minion: Minion) = mockk<MinionTriggerDispatch>(relaxed = true) {
        every { storeAndDispatchCollect(minion, any()) } answers { minion.storage.store(secondArg()) }
    }

    private fun activity(
        minion: Minion,
        events: EventDispatcher = RecordingEventDispatcher(),
        isMinionItem: (ItemStack) -> Boolean = { false },
        canWork: (Minion, Block) -> Boolean = { _, _ -> true }
    ) = CollectionActivity(entities, dispatch(minion), events, sink, isMinionItem, listening = { true }, canWork = canWork)

    @Test
    fun `leaves items and orbs where the owner cannot build`() {
        val subject = minion()
        val dropped = item(ItemStack(Material.COBBLESTONE))
        items += dropped
        orbs += mockk<ExperienceOrb>(relaxed = true) { every { isValid } returns true; every { experience } returns 3 }

        assertEquals(ActivityResult.BUSY, activity(subject, canWork = { _, _ -> false }).act(subject, 0L))

        verify(exactly = 0) { dropped.remove() }
        verify(exactly = 0) { sink.store(any(), 3) }
    }

    private fun item(stack: ItemStack, pickupDelay: Int = 0): Item {
        var current = stack
        return mockk(relaxed = true) {
            every { isValid } returns true
            every { this@mockk.pickupDelay } returns pickupDelay
            every { itemStack } answers { current }
            every { itemStack = any() } answers { current = firstArg() }
        }
    }

    @Test
    fun `collects a ready item into storage`() {
        val subject = minion()
        val dropped = item(ItemStack(Material.COBBLESTONE, 10))
        items += dropped

        assertEquals(ActivityResult.WORKED, activity(subject).act(subject, 0L))

        assertEquals(10, subject.storage.count { it.type == Material.COBBLESTONE })
        verify { dropped.remove() }
    }

    @Test
    fun `skips an item that still has a pickup delay`() {
        val subject = minion()
        items += item(ItemStack(Material.COBBLESTONE), pickupDelay = 10)

        assertEquals(ActivityResult.BUSY, activity(subject).act(subject, 0L))
    }

    @Test
    fun `skips a minion item`() {
        val subject = minion()
        items += item(ItemStack(Material.ARMOR_STAND))

        assertEquals(ActivityResult.BUSY, activity(subject, isMinionItem = { it.type == Material.ARMOR_STAND }).act(subject, 0L))
    }

    @Test
    fun `respects the target item filter`() {
        val subject = minion(CollectionSettings(listOf(FakeItemLookup.testable(Material.DIAMOND)), true))
        items += item(ItemStack(Material.COBBLESTONE))

        assertEquals(ActivityResult.BUSY, activity(subject).act(subject, 0L))
    }

    @Test
    fun `a stack that only partly fits is split`() {
        val subject = minion()
        subject.storage.add(ItemStack(Material.COBBLESTONE, 54))
        val dropped = item(ItemStack(Material.COBBLESTONE, 64))
        items += dropped

        assertEquals(ActivityResult.WORKED, activity(subject).act(subject, 0L))

        assertEquals(54, dropped.itemStack.amount)
        verify(exactly = 0) { dropped.remove() }
    }

    @Test
    fun `absorbs experience orbs`() {
        val subject = minion()
        val first = mockk<ExperienceOrb>(relaxed = true) { every { isValid } returns true; every { experience } returns 3 }
        val second = mockk<ExperienceOrb>(relaxed = true) { every { isValid } returns true; every { experience } returns 4 }
        orbs += listOf(first, second)

        assertEquals(ActivityResult.WORKED, activity(subject).act(subject, 0L))

        verify { sink.store(subject, 7) }
        verify { first.remove() }
        verify { second.remove() }
    }

    @Test
    fun `nothing in range waits with nothing-to-collect`() {
        val subject = minion()

        assertEquals(ActivityResult.BUSY, activity(subject).act(subject, 0L))
        assertEquals("nothing-to-collect", subject.blockedReason)
    }

    @Test
    fun `a cancelled collect event leaves the item`() {
        val subject = minion()
        val dropped = item(ItemStack(Material.COBBLESTONE))
        items += dropped

        activity(subject, RecordingEventDispatcher(allow = false)).act(subject, 0L)

        verify(exactly = 0) { dropped.remove() }
        assertEquals(0, subject.storage.count { true })
    }

    @Test
    fun `no offline progress`() {
        val subject = minion()

        assertNull(activity(subject).offlineCycle(subject))
    }
}
