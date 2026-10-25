package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionBreedEvent
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionBreed
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.BreederSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import com.exanthiax.ecominions.testsupport.FakeEntityLookup
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import com.exanthiax.ecominions.testsupport.FakePersistentDataContainer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Animals
import org.bukkit.entity.Cow
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pig
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class BreederActivityTest {

    private val spawnedByKey = NamespacedKey("ecominions", "minion")
    private val animals = mutableListOf<Animals>()
    private val entities = object : WorkAreaEntities {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Entity> find(minion: Minion, type: Class<T>): List<T> =
            if (type == Animals::class.java) animals as List<T> else emptyList()
    }

    private val baseStats = MinionDefinition.BaseStats(
        speedTicks = "20",
        efficiencyTicks = "5",
        radius = "2",
        storageSlots = "3",
        maxStorageSlots = "54",
        hardSlotCap = 54,
        regenDelayTicks = "60"
    )
    private val stats = MinionStats(baseStats, level = 1)

    private fun settings(maxMobs: String = "8") = BreederSettings(
        targets = listOf(
            FakeEntityLookup.testable(EntityType.COW),
            FakeEntityLookup.testable(EntityType.PIG)
        ),
        food = listOf(
            FakeItemLookup.testable(Material.WHEAT),
            FakeItemLookup.testable(Material.CARROT)
        ),
        maxMobs = maxMobs
    )

    private fun breeder(vararg stored: ItemStack, maxMobs: String = "8") = Minion(
        state = MinionState(
            uuid = UUID.randomUUID(),
            definition = MinionDefinitionFixture.build(
                type = MinionType.BREEDER,
                baseStats = baseStats,
                breeder = settings(maxMobs)
            )
        ),
        entity = mockk(relaxed = true) { every { location } returns mockk<Location>(relaxed = true) },
        storage = MinionStorage(3) { a, b -> a.type == b.type },
        stats = stats
    ).also { subject -> stored.forEach { subject.storage.add(it) } }

    private fun cow(
        ready: Boolean = true,
        adult: Boolean = true,
        named: Boolean = false,
        inLove: Boolean = false,
        tagged: Boolean = false
    ): Cow = mockk<Cow>(relaxed = true) {
        every { type } returns EntityType.COW
        every { canBreed() } returns ready
        every { isAdult } returns adult
        every { isLoveMode } returns inLove
        every { isBreedItem(any<ItemStack>()) } answers { firstArg<ItemStack>().type == Material.WHEAT }
        every { customName() } returns if (named) Component.text("Bob") else null
        every { persistentDataContainer } returns tagContainer(tagged)
        every { location } returns mockk<Location>(relaxed = true)
    }.also { animals += it }

    private fun forager(): Cow = mockk<Cow>(relaxed = true) {
        every { type } returns EntityType.COW
        every { canBreed() } returns true
        every { isAdult } returns true
        every { isLoveMode } returns false
        every { isBreedItem(any<ItemStack>()) } answers {
            firstArg<ItemStack>().type in setOf(Material.WHEAT, Material.CARROT)
        }
        every { customName() } returns null
        every { persistentDataContainer } returns tagContainer(false)
        every { location } returns mockk<Location>(relaxed = true)
    }.also { animals += it }

    private fun pig(): Pig = mockk<Pig>(relaxed = true) {
        every { type } returns EntityType.PIG
        every { canBreed() } returns true
        every { isAdult } returns true
        every { isLoveMode } returns false
        every { isBreedItem(any<ItemStack>()) } answers { firstArg<ItemStack>().type == Material.CARROT }
        every { customName() } returns null
        every { persistentDataContainer } returns tagContainer(false)
        every { location } returns mockk<Location>(relaxed = true)
    }.also { animals += it }

    private fun tagContainer(tagged: Boolean) = FakePersistentDataContainer.create().also { container ->
        if (tagged) {
            container.set(spawnedByKey, PersistentDataType.STRING, UUID.randomUUID().toString())
        }
    }

    private val dispatch = mockk<MinionTriggerDispatch>(relaxed = true)

    private fun activity(
        saved: (Minion) -> Unit = {},
        events: EventDispatcher = RecordingEventDispatcher(),
        listening: Boolean = true,
        canWork: (Minion, Block) -> Boolean = { _, _ -> true }
    ) = BreederActivity(
        entities = entities,
        storageSaved = saved,
        spawnedByKey = spawnedByKey,
        signals = ActivitySignals(events, dispatch) { listening },
        random = Random(1),
        canWork = canWork
    )

    private fun count(minion: Minion, material: Material) = minion.storage.count { it.type == material }

    private fun assertNothingToBreed(subject: Minion, result: ActivityResult) {
        assertEquals(ActivityResult.BUSY, result)
        assertEquals("nothing-to-breed", subject.blockedReason)
        assertEquals(5, count(subject, Material.WHEAT))
    }

    @Test
    fun `breeds a pair with two food`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        val first = cow()
        val second = cow()
        var saves = 0

        assertEquals(ActivityResult.WORKED, activity(saved = { saves++ }).act(subject, 0L))

        verify { first.loveModeTicks = 600 }
        verify { second.loveModeTicks = 600 }
        assertEquals(3, count(subject, Material.WHEAT))
        assertEquals(1, saves)
    }

    @Test
    fun `cancelled breed keeps the food`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        val first = cow()
        val second = cow()
        var saves = 0

        val result = activity(saved = { saves++ }, events = RecordingEventDispatcher(allow = false)).act(subject, 0L)

        assertNothingToBreed(subject, result)
        verify(exactly = 0) { first.loveModeTicks = any() }
        verify(exactly = 0) { second.loveModeTicks = any() }
        assertEquals(0, saves)
        verify(exactly = 0) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `breed event carries the pair`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        val first = cow()
        val second = cow()
        val events = RecordingEventDispatcher()

        activity(events = events).act(subject, 0L)

        val event = events.last<MinionBreedEvent>()
        assertSame(subject, event.minion)
        assertEquals(setOf(first, second), setOf(event.first, event.second))
        assertEquals(Material.WHEAT, event.food.type)
    }

    @Test
    fun `breed event needs a listener`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow()
        val events = RecordingEventDispatcher()

        assertEquals(ActivityResult.WORKED, activity(events = events, listening = false).act(subject, 0L))

        assertEquals(emptyList<Any>(), events.events)
    }

    @Test
    fun `fires the breed trigger once`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow()
        val events = RecordingEventDispatcher()

        activity(events = events).act(subject, 0L)

        val spot = events.last<MinionBreedEvent>().first.location
        verify(exactly = 1) { dispatch.triggerAt(subject, TriggerMinionBreed, spot, 2.0) }
        verify(exactly = 1) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `needs a pair`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `uses only food the animal eats`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        pig()
        pig()

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `breeds another kind`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5), ItemStack(Material.CARROT, 4))
        val first = pig()
        val second = pig()

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify { first.loveModeTicks = 600 }
        verify { second.loveModeTicks = 600 }
        assertEquals(2, count(subject, Material.CARROT))
        assertEquals(5, count(subject, Material.WHEAT))
    }

    @Test
    fun `needs two of the same food`() {
        val subject = breeder(ItemStack(Material.WHEAT, 1))
        cow()
        cow()

        val events = RecordingEventDispatcher()

        assertEquals(ActivityResult.BUSY, activity(events = events).act(subject, 0L))
        assertEquals(1, count(subject, Material.WHEAT))
        assertEquals(emptyList<Any>(), events.events)
    }

    @Test
    fun `stray food hides no stack`() {
        val subject = breeder(ItemStack(Material.WHEAT, 1), ItemStack(Material.CARROT, 64))
        val first = forager()
        val second = forager()

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify { first.loveModeTicks = 600 }
        verify { second.loveModeTicks = 600 }
        assertEquals(1, count(subject, Material.WHEAT))
        assertEquals(62, count(subject, Material.CARROT))
    }

    @Test
    fun `breed event food amount is two`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow()
        val events = RecordingEventDispatcher()

        activity(events = events).act(subject, 0L)

        assertEquals(2, events.last<MinionBreedEvent>().food.amount)
    }

    @Test
    fun `max-mobs counts babies`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        repeat(2) { cow() }
        repeat(6) { cow(adult = false) }

        assertEquals(ActivityResult.BUSY, activity().act(subject, 0L))

        assertEquals("pen-full", subject.blockedReason)
        assertEquals(5, count(subject, Material.WHEAT))
    }

    @Test
    fun `breeds below max-mobs`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        repeat(2) { cow() }
        repeat(5) { cow(adult = false) }

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))
    }

    @Test
    fun `skips named animals`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow(named = true)

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `skips animals already in love`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow(inLove = true)

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `skips unbreedable animals`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow(ready = false)

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `skips slayer-tagged mobs`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow(tagged = true)

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `skips babies`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow(adult = false)

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `skips mobs not targeted`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        mockk<Cow>(relaxed = true) {
            every { type } returns EntityType.MOOSHROOM
            every { canBreed() } returns true
            every { isAdult } returns true
            every { isLoveMode } returns false
            every { isBreedItem(any<ItemStack>()) } returns true
            every { customName() } returns null
            every { persistentDataContainer } returns tagContainer(false)
            every { location } returns mockk<Location>(relaxed = true)
        }.also { animals += it }

        assertNothingToBreed(subject, activity().act(subject, 0L))
    }

    @Test
    fun `skips animals owner cannot use`() {
        val subject = breeder(ItemStack(Material.WHEAT, 5))
        cow()
        cow()

        assertNothingToBreed(subject, activity(canWork = { _, _ -> false }).act(subject, 0L))
    }

    @Test
    fun `blocked with an empty store`() {
        val subject = breeder()
        cow()
        cow()

        assertEquals(ActivityResult.BLOCKED, activity().act(subject, 0L))
        assertEquals("no-food-stored", subject.blockedReason)
    }

    @Test
    fun `ignores items no longer food`() {
        val subject = breeder(ItemStack(Material.STONE, 4))
        cow()
        cow()

        assertEquals(ActivityResult.BLOCKED, activity().act(subject, 0L))
        assertEquals("no-food-stored", subject.blockedReason)
        assertEquals(4, count(subject, Material.STONE))
    }

    @Test
    fun `offline cycle earns nothing`() {
        assertNull(activity().offlineCycle(breeder(ItemStack(Material.WHEAT, 5))))
    }
}
